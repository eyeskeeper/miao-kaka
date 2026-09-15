package com.senze.miaokaka.service;

import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelMember;

/**
 * 死斗结算服务（幂等）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface DuelSettlementService {

    /**
     * 结算单个死斗：结束日已过且未结算时执行。
     * 含"结束时任然待审核的记录自动视为通过"条款。
     *
     * @return 是否实际执行了结算
     */
    boolean settleIfDue(Long duelId);

    /**
     * 扫描并结算所有到期死斗（定时任务用）
     */
    void settleAllDue();

    /**
     * 每日即退：打卡提交当天立即发放当日份额 floor(押金/T) 并累计成员 refunded。
     * 审核驳回时由 clawbackDaily 追回。
     */
    void dailyRefund(Duel duel, DuelMember member);

    /**
     * 凭证驳回追回：扣回打卡时已发放的当日份额（允许余额临时为负，保证账目精确）
     */
    void clawbackDaily(Duel duel, DuelMember member);
}
