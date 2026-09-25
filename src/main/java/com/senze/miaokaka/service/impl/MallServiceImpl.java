package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.MallConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.mapper.UserItemMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.entity.UserItem;
import com.senze.miaokaka.model.vo.BagItemVO;
import com.senze.miaokaka.model.vo.MallBuyResultVO;
import com.senze.miaokaka.model.vo.MallItemVO;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.MallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 积分商城服务实现。
 * 积分扣减用条件更新（total_points >= price 原子扣减，防并发透支），
 * 背包数量条件更新保证不为负；积分变动后逐出用户缓存（与既有积分模式一致）。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MallServiceImpl extends ServiceImpl<UserItemMapper, UserItem> implements MallService {

    private final UserItemMapper userItemMapper;

    private final UserMapper userMapper;

    private final CacheService cacheService;

    @Override
    public List<MallItemVO> catalog(Long userId) {
        List<MallItemVO> vos = new ArrayList<>();
        for (String code : MallConstant.ALL_ITEMS) {
            MallItemVO vo = new MallItemVO();
            vo.setCode(code);
            vo.setName(MallConstant.nameOf(code));
            vo.setPrice(MallConstant.priceOf(code));
            vo.setDescription(MallConstant.descOf(code));
            vo.setOwned(ownedCount(userId, code));
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public List<BagItemVO> bag(Long userId) {
        List<BagItemVO> vos = new ArrayList<>();
        for (String code : MallConstant.ALL_ITEMS) {
            int owned = ownedCount(userId, code);
            if (owned <= 0) {
                continue;
            }
            BagItemVO vo = new BagItemVO();
            vo.setCode(code);
            vo.setName(MallConstant.nameOf(code));
            vo.setQuantity(owned);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public MallBuyResultVO buy(Long userId, String itemCode) {
        int price = MallConstant.priceOf(itemCode);
        // 原子扣积分：余额不足直接拒绝（条件更新防并发透支）
        boolean deducted = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .ge(User::getTotalPoints, price)
                .setSql("total_points = total_points - " + price)) > 0;
        if (!deducted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "积分不足，购买「" + MallConstant.nameOf(itemCode) + "」需要 " + price + " 积分");
        }
        addItem(userId, itemCode, 1);
        // 积分已变：逐出用户缓存
        cacheService.evict(CacheService.keyUser(userId));
        MallBuyResultVO vo = new MallBuyResultVO();
        vo.setTotalPoints(walletAfter(userId));
        vo.setOwned(ownedCount(userId, itemCode));
        log.info("用户 {} 购买道具 {}（{} 积分）", userId, itemCode, price);
        return vo;
    }

    @Override
    public boolean consumeItem(Long userId, String itemCode) {
        boolean consumed = userItemMapper.update(null, new LambdaUpdateWrapper<UserItem>()
                .eq(UserItem::getUserId, userId)
                .eq(UserItem::getItemCode, itemCode)
                .gt(UserItem::getQuantity, 0)
                .setSql("quantity = quantity - 1")) > 0;
        if (consumed) {
            log.info("用户 {} 消耗道具 {}", userId, itemCode);
        }
        return consumed;
    }

    private void addItem(Long userId, String itemCode, int delta) {
        UserItem existing = getOne(new LambdaQueryWrapper<UserItem>()
                .eq(UserItem::getUserId, userId)
                .eq(UserItem::getItemCode, itemCode));
        if (existing == null) {
            UserItem item = new UserItem();
            item.setUserId(userId);
            item.setItemCode(itemCode);
            item.setQuantity(delta);
            try {
                save(item);
                return;
            } catch (DuplicateKeyException e) {
                // 并发首购撞唯一键：落回数量累加
            }
        }
        userItemMapper.update(null, new LambdaUpdateWrapper<UserItem>()
                .eq(UserItem::getUserId, userId)
                .eq(UserItem::getItemCode, itemCode)
                .setSql("quantity = quantity + " + delta));
    }

    private int ownedCount(Long userId, String itemCode) {
        UserItem item = getOne(new LambdaQueryWrapper<UserItem>()
                .eq(UserItem::getUserId, userId)
                .eq(UserItem::getItemCode, itemCode));
        return item == null || item.getQuantity() == null ? 0 : item.getQuantity();
    }

    private int walletAfter(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null || user.getTotalPoints() == null ? 0 : user.getTotalPoints();
    }
}
