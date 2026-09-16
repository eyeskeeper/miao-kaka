package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 邀请码落地信息（免登录，扫码可见）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class InviteInfoVO implements Serializable {

    private Long duelId;

    private String duelName;

    private String leaderName;

    /**
     * 加入模式 (0:自由加入, 1:审批加入)
     */
    private Integer joinMode;

    private Integer depositPerMember;

    private Integer totalDays;

    /**
     * 当前正式成员数
     */
    private Integer memberCount;

    /**
     * 死斗状态 (0:招募中, 1:进行中, 2:已结算, 3:已解散)
     */
    private Integer status;

    /**
     * 邀请人昵称
     */
    private String inviterName;

    /**
     * 是否组长邀请（组长码免审直接入组）
     */
    private Boolean leaderInvite;

    /**
     * 使用该码的动作：direct=直接加入 / apply=需组长审批
     */
    private String joinAction;

    private static final long serialVersionUID = 1L;
}
