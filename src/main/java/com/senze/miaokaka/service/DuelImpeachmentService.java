package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.dto.duel.ImpeachInitiateRequest;
import com.senze.miaokaka.model.dto.duel.ImpeachmentVoteRequest;
import com.senze.miaokaka.model.entity.DuelImpeachment;
import com.senze.miaokaka.model.vo.DuelVO;

/**
 * 死斗弹劾组长服务（发起/投票/到期结算/详情装配）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface DuelImpeachmentService extends IService<DuelImpeachment> {

    /**
     * 发起弹劾：仅进行中死斗的正式成员（非组长）；原因≤20字；同一时刻仅一个进行中弹劾；
     * 失败后冷却 24 小时；发起人自动记 1 张弹劾票
     */
    void initiate(Long userId, Long duelId, ImpeachInitiateRequest request);

    /**
     * 弹劾投票：正式成员（含组长，组长只能投维持）对进行中弹劾投 维持(0)/弹劾(1)；
     * 一票定死不可改；弹劾票严格过半即成功，发起人立即成为新组长
     */
    void vote(Long userId, Long duelId, ImpeachmentVoteRequest request);

    /**
     * 结算所有过期弹劾（定时任务扫期），返回本次判负数量
     */
    int resolveExpired();

    /**
     * 组装进行中弹劾概要（观看者无关，进聚合缓存）；过期未结算的当场惰性判负并返回 null
     */
    DuelVO.ImpeachmentVO assembleActiveVO(Long duelId, Integer totalCount);

    /**
     * 该死斗当前进行中弹劾的发起人 id（无进行中弹劾返回 null）
     */
    Long getActiveInitiator(Long duelId);

    /**
     * 组长变更（让渡）时终止进行中的弹劾：弹劾对象已换人，按失败留痕
     */
    void terminateOnLeaderChange(Long duelId);

    /**
     * 我对某场弹劾的投票：null 未投 / 0 维持 / 1 弹劾
     */
    Integer myVoteOf(Long impeachmentId, Long userId);
}
