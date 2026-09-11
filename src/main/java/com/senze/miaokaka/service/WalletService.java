package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.entity.CoinTransaction;
import com.senze.miaokaka.model.vo.WalletVO;

/**
 * 喵币钱包服务：余额只随流水变动，每笔记录 balance_after
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface WalletService extends IService<CoinTransaction> {

    /**
     * 注册赠送喵币（仅注册流程调用）
     */
    void grantRegisterGift(Long userId);

    /**
     * 扣押金（原子条件更新，余额不足抛业务异常）
     */
    void chargeDeposit(Long userId, int amount, Long duelId);

    /**
     * 退还（开始前退出 / 结算自退）
     */
    void refund(Long userId, int amount, Long duelId, String remark);

    /**
     * 奖池分得
     */
    void awardPoolShare(Long userId, int amount, Long duelId);

    /**
     * 当前余额
     */
    int getBalance(Long userId);

    /**
     * 余额 + 流水分页
     */
    WalletVO walletDetail(Long userId, int current, int pageSize);
}
