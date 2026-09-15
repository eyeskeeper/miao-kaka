package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
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
     * 组长视角：待审核凭证数
     */
    private Long pendingCount;

    private List<MemberVO> members;

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
