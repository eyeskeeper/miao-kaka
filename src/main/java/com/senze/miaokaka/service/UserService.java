package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.dto.admin.UserBanRequest;
import com.senze.miaokaka.model.dto.admin.UserPageQueryRequest;
import com.senze.miaokaka.model.dto.user.UserLoginRequest;
import com.senze.miaokaka.model.dto.user.UserRegisterRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.LoginResponseVO;
import com.senze.miaokaka.model.vo.LoginUserVO;
import com.senze.miaokaka.model.vo.RankBoardVO;
import com.senze.miaokaka.model.vo.RankItemVO;
import com.senze.miaokaka.model.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户服务
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface UserService extends IService<User> {

    /**
     * 注册
     *
     * @return 新用户id
     */
    long register(UserRegisterRequest request);

    /**
     * 账号密码登录，签发 JWT
     */
    LoginResponseVO login(UserLoginRequest request);

    /**
     * 从请求属性中取当前登录用户（由 JwtInterceptor 写入）
     */
    User getLoginUser(HttpServletRequest request);

    LoginUserVO toLoginUserVO(User user);

    /**
     * 管理端分页查用户
     */
    Page<UserVO> pageUserVOs(UserPageQueryRequest request);

    /**
     * 封禁/解封用户（admin 不可被封禁）
     */
    boolean banUser(UserBanRequest request);

    /**
     * 全勤连击榜：Top 50（走缓存）+ 当前用户排名 myRank
     * （Top 50 内取榜单名次；50 外按同口径实时补算；零连击为 null）
     */
    RankBoardVO streakBoard(Long userId);

    /**
     * 管理员建号（赠喵币，与注册一致）
     */
    long createUser(com.senze.miaokaka.model.dto.admin.AdminUserCreateRequest request);

    /**
     * 管理员查用户详情
     */
    UserVO getUserDetail(Long userId);

    /**
     * 管理员改资料/角色/重置密码（仅更新传入字段；角色仅 user↔admin）
     */
    UserVO updateUserDetail(Long operatorId, Long userId,
                            com.senze.miaokaka.model.dto.admin.AdminUserUpdateRequest request);

    /**
     * 管理员删除用户（逻辑删除 + 账号归档改名；名下有招募中/进行中死斗押金时拒绝）
     */
    void deleteUser(Long operatorId, Long userId);
}
