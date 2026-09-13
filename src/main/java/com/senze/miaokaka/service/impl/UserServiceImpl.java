package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.GameConstants;
import com.senze.miaokaka.constant.UserConstant;
import com.senze.miaokaka.config.JwtProperties;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.admin.UserBanRequest;
import com.senze.miaokaka.model.dto.admin.UserPageQueryRequest;
import com.senze.miaokaka.model.dto.user.UserLoginRequest;
import com.senze.miaokaka.model.dto.user.UserRegisterRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.LoginResponseVO;
import com.senze.miaokaka.model.vo.LoginUserVO;
import com.senze.miaokaka.model.vo.RankBoardVO;
import com.senze.miaokaka.model.vo.RankCacheData;
import com.senze.miaokaka.model.vo.RankItemVO;
import com.senze.miaokaka.model.vo.UserVO;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.UserService;
import com.senze.miaokaka.service.WalletService;
import com.senze.miaokaka.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final JwtProperties jwtProperties;

    private final WalletService walletService;

    private final CacheService cacheService;

    @Override
    public long register(UserRegisterRequest request) {
        ThrowUtils.throwIf(!StrUtil.equals(request.getUserPassword(), request.getCheckPassword()),
                ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        String account = request.getUserAccount().trim();
        long exists = count(new LambdaQueryWrapper<User>().eq(User::getUserAccount, account));
        ThrowUtils.throwIf(exists > 0, ErrorCode.PARAMS_ERROR, "该账号已被注册");
        User user = new User();
        user.setUserAccount(account);
        user.setUserPassword(PASSWORD_ENCODER.encode(request.getUserPassword()));
        user.setUserName("喵友" + RandomUtil.randomNumbers(6));
        user.setUserRole(UserConstant.DEFAULT_ROLE);
        user.setCurrentStreak(0);
        user.setTotalPoints(0);
        user.setMiaoCoins(0);
        boolean saved = save(user);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "注册失败，请重试");
        // 注册赠送喵币（押金货币）
        walletService.grantRegisterGift(user.getId());
        return user.getId();
    }

    @Override
    public LoginResponseVO login(UserLoginRequest request) {
        String account = request.getUserAccount().trim();
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUserAccount, account));
        ThrowUtils.throwIf(user == null || !PASSWORD_ENCODER.matches(request.getUserPassword(), user.getUserPassword()),
                ErrorCode.PARAMS_ERROR, "账号或密码错误");
        ThrowUtils.throwIf(UserConstant.BAN_ROLE.equals(user.getUserRole()),
                ErrorCode.NO_AUTH_ERROR, "账号已被封禁，请联系管理员");
        String token = JwtUtils.createToken(user.getId(), user.getUserRole(),
                jwtProperties.getSecret(), jwtProperties.getExpireDays());
        return new LoginResponseVO(token, toLoginUserVO(user));
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        User user = (User) request.getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        return user;
    }

    @Override
    public LoginUserVO toLoginUserVO(User user) {
        LoginUserVO vo = new LoginUserVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserAvatar(user.getUserAvatar());
        vo.setUserProfile(user.getUserProfile());
        vo.setUserRole(user.getUserRole());
        vo.setCurrentStreak(user.getCurrentStreak());
        vo.setTotalPoints(user.getTotalPoints());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public Page<UserVO> pageUserVOs(UserPageQueryRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getUserName()), User::getUserName, request.getUserName());
        wrapper.eq(StrUtil.isNotBlank(request.getUserRole()), User::getUserRole, request.getUserRole());
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> page = page(new Page<>(request.getCurrent(), request.getPageSize()), wrapper);
        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toUserVO).toList());
        return voPage;
    }

    @Override
    public boolean banUser(UserBanRequest request) {
        User target = getById(request.getUserId());
        ThrowUtils.throwIf(target == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        ThrowUtils.throwIf(UserConstant.ADMIN_ROLE.equals(target.getUserRole()),
                ErrorCode.NO_AUTH_ERROR, "不能封禁管理员账号");
        target.setUserRole(Boolean.TRUE.equals(request.getIsBan())
                ? UserConstant.BAN_ROLE
                : UserConstant.DEFAULT_ROLE);
        boolean updated = updateById(target);
        if (updated) {
            // 逐出登录态缓存 → 封禁毫秒级生效；连击榜同步剔除
            cacheService.evict(CacheService.keyUser(target.getId()), CacheService.KEY_RANK_STREAK);
        }
        return updated;
    }

    @Override
    public RankBoardVO streakBoard(Long userId) {
        List<RankItemVO> items = loadStreakTop50();
        RankBoardVO board = new RankBoardVO();
        board.setList(items);
        board.setMyRank(myStreakRank(userId, items));
        return board;
    }

    /**
     * 当前用户排名：Top 50 内直接取榜单名次；50 外按与榜单完全相同的口径
     * （连击降序、id 升序、排除封禁）实时补算；零连击无排名
     */
    private Integer myStreakRank(Long userId, List<RankItemVO> items) {
        for (RankItemVO item : items) {
            if (item.getUserId().equals(userId)) {
                return item.getRank();
            }
        }
        User me = getById(userId);
        if (me == null || me.getCurrentStreak() == null || me.getCurrentStreak() <= 0) {
            return null;
        }
        int streak = me.getCurrentStreak();
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.ne("user_role", UserConstant.BAN_ROLE);
        wrapper.and(q -> q.gt("current_streak", streak)
                .or(o -> o.eq("current_streak", streak).lt("id", userId)));
        return (int) count(wrapper) + 1;
    }

    private List<RankItemVO> loadStreakTop50() {
        RankCacheData cached = cacheService.get(CacheService.KEY_RANK_STREAK, RankCacheData.class);
        if (cached != null && cached.getItems() != null) {
            return cached.getItems();
        }
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.ne("user_role", UserConstant.BAN_ROLE);
        wrapper.gt("current_streak", 0);
        wrapper.orderByDesc("current_streak");
        wrapper.orderByAsc("id");
        wrapper.last("LIMIT " + GameConstants.RANK_TOP_SIZE);
        List<User> users = list(wrapper);
        List<RankItemVO> result = new java.util.ArrayList<>(users.size());
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            RankItemVO vo = new RankItemVO();
            vo.setRank(i + 1);
            vo.setUserId(user.getId());
            vo.setUserName(user.getUserName());
            vo.setUserAvatar(user.getUserAvatar());
            vo.setCurrentStreak(user.getCurrentStreak());
            result.add(vo);
        }
        RankCacheData cacheData = new RankCacheData();
        cacheData.setItems(result);
        cacheService.put(CacheService.KEY_RANK_STREAK, cacheData, java.time.Duration.ofMinutes(5));
        return result;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserAvatar(user.getUserAvatar());
        vo.setUserRole(user.getUserRole());
        vo.setCurrentStreak(user.getCurrentStreak());
        vo.setTotalPoints(user.getTotalPoints());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
