package com.senze.miaokaka.service;

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
}
