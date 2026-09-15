package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.CoinTransactionMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.entity.CoinTransaction;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.WalletVO;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 喵币钱包服务实现
 * 扣款用"条件原子更新"，行级并发天然串行；每笔写 balance_after 供重放对账
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl extends ServiceImpl<CoinTransactionMapper, CoinTransaction>
        implements WalletService {

    private final UserMapper userMapper;

    private final CacheService cacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantRegisterGift(Long userId) {
        addBalance(userId, DuelConstant.REGISTER_GIFT_COINS);
        record(userId, DuelConstant.COIN_TX_REGISTER_GIFT, DuelConstant.REGISTER_GIFT_COINS,
                null, "注册赠送喵币");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void chargeDeposit(Long userId, int amount, Long duelId) {
        ThrowUtils.throwIf(amount <= 0, ErrorCode.PARAMS_ERROR, "押金数额不合法");
        // 原子条件更新：余额不足时影响行数为 0
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.eq(User::getId, userId)
                .ge(User::getMiaoCoins, amount)
                .setSql("miao_coins = miao_coins - " + amount);
        int rows = userMapper.update(null, uw);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "喵币余额不足（需 " + amount + " 喵币）");
        }
        cacheService.evict(CacheService.keyUser(userId));
        record(userId, DuelConstant.COIN_TX_DEPOSIT, -amount, duelId, "死斗押金托管");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, int amount, Long duelId, String remark) {
        if (amount <= 0) {
            return;
        }
        addBalance(userId, amount);
        record(userId, DuelConstant.COIN_TX_REFUND, amount, duelId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void awardPoolShare(Long userId, int amount, Long duelId) {
        if (amount <= 0) {
            return;
        }
        addBalance(userId, amount);
        record(userId, DuelConstant.COIN_TX_POOL_SHARE, amount, duelId, "死斗奖池分得");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dailyRefund(Long userId, int amount, Long duelId) {
        if (amount <= 0) {
            return;
        }
        addBalance(userId, amount);
        record(userId, DuelConstant.COIN_TX_DAILY_REFUND, amount, duelId, "死斗每日打卡退还");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clawbackDaily(Long userId, int amount, Long duelId) {
        if (amount <= 0) {
            return;
        }
        // 无条件扣减：追回场景允许余额临时为负，保证账目精确
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.eq(User::getId, userId)
                .setSql("miao_coins = miao_coins - " + amount);
        int rows = userMapper.update(null, uw);
        ThrowUtils.throwIf(rows == 0, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        cacheService.evict(CacheService.keyUser(userId));
        record(userId, DuelConstant.COIN_TX_DAILY_CLAWBACK, -amount, duelId, "凭证驳回，追回当日奖励");
    }

    @Override
    public int getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return user.getMiaoCoins() == null ? 0 : user.getMiaoCoins();
    }

    @Override
    public WalletVO walletDetail(Long userId, int current, int pageSize) {
        WalletVO vo = new WalletVO();
        vo.setBalance(getBalance(userId));
        Page<CoinTransaction> page = page(new Page<>(current, pageSize),
                new LambdaQueryWrapper<CoinTransaction>()
                        .eq(CoinTransaction::getUserId, userId)
                        .orderByDesc(CoinTransaction::getId));
        vo.setTransactions(page);
        return vo;
    }

    /**
     * 加余额（退款/奖池/赠送为正数路径）
     */
    /**
     * 加余额（退款/奖池/赠送为正数路径）；喵币属钱的数据不缓存，
     * 但登录态用户对象含余额字段，需随写逐出保证 /me 实时
     */
    private void addBalance(Long userId, int amount) {
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.eq(User::getId, userId)
                .setSql("miao_coins = miao_coins + " + amount);
        int rows = userMapper.update(null, uw);
        ThrowUtils.throwIf(rows == 0, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        cacheService.evict(CacheService.keyUser(userId));
    }

    /**
     * 写流水（balance_after 以变动后的实际余额为准）
     */
    private void record(Long userId, int type, int amount, Long bizId, String remark) {
        CoinTransaction tx = new CoinTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBizId(bizId);
        tx.setRemark(remark);
        tx.setBalanceAfter(getBalance(userId));
        save(tx);
    }
}
