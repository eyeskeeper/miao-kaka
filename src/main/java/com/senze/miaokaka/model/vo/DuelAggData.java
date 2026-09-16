package com.senze.miaokaka.model.vo;

import com.senze.miaokaka.model.entity.Duel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 死斗聚合缓存数据（观看者无关层）。
 * 注意：myRole/myStatus/myPlanId/pendingCount 等个性化字段绝不进缓存，
 * 否则会把 A 的视角数据泄漏给 B。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class DuelAggData implements Serializable {

    private Duel duel;

    private List<MemberLine> members;

    /**
     * 进行中的弹劾概要（无则 null；发起/投票/终止时逐出缓存刷新）
     */
    private DuelVO.ImpeachmentVO impeachment;

    @Data
    public static class MemberLine implements Serializable {

        private Long userId;

        private String userName;

        private String userAvatar;

        private boolean leader;

        private int deposit;

        /**
         * 已确认打卡天数（按缓存时刻计算）
         */
        private int days;

        /**
         * 0:已加入, 1:进行中, 2:已结算, 3:已退出
         */
        private int status;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
