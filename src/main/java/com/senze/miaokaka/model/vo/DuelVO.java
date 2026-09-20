package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 死斗视图
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class DuelVO implements Serializable {

    private Long id;

    private String duelName;

    private String duelDesc;

    /**
     * 每日任务清单（无任务清单的局为 null）
     */
    private List<String> dailyTasks;

    private Long leaderId;

    /**
     * 加入模式 (0:自由加入, 1:审批加入)
     */
    private Integer joinMode;

    private Integer depositPerMember;

    private Integer totalDays;

    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * 0:招募中, 1:进行中, 2:已结算, 3:已解散
     */
    private Integer status;

    private Integer memberCount;

    private Integer totalPool;

    private Boolean settled;

    /**
     * 我在其中的角色：leader / member / null（未参与）
     */
    private String myRole;

    /**
     * 我的成员状态（参与时）
     */
    private Integer myStatus;

    /**
     * 我的影子计划id（参与时）
     */
    private Long myPlanId;

    /**
     * 我的加入申请状态：null 无申请 / 0 待审 / 1 已通过 / 2 已拒绝（本人最新一条申请，实时查）
     */
    private Integer myApplyStatus;

    /**
     * 组长视角：待审核凭证数
     */
    private Long pendingCount;

    /**
     * 进行中的弹劾（无则 null，已结束的不展示）
     */
    private ImpeachmentVO impeachment;

    /**
     * 我对进行中弹劾的投票：null 未投 / 0 维持 / 1 弹劾
     */
    private Integer myImpeachVote;

    private List<MemberVO> members;

    /**
     * 进行中的弹劾概要（观看者无关，进聚合缓存）
     */
    @Data
    public static class ImpeachmentVO implements Serializable {

        private Long id;

        private Long initiatorId;

        private String initiatorName;

        /**
         * 弹劾原因（20字内）
         */
        private String reason;

        private Integer impeachCount;

        private Integer maintainCount;

        /**
         * 投票基数（当前正式成员数，弹劾票严格过半即成功）
         */
        private Integer totalCount;

        /**
         * 投票截止时间
         */
        private Date expireTime;

        private static final long serialVersionUID = 1L;
    }

    @Data
    public static class MemberVO implements Serializable {

        private Long userId;

        private String userName;

        private String userAvatar;

        private Boolean isLeader;

        /**
         * 已确认打卡天数
         */
        private Integer days;

        private Integer deposit;

        /**
         * 0:已加入, 1:进行中, 2:已结算, 3:已退出
         */
        private Integer status;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
