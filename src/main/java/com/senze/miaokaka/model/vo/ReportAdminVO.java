package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * admin 举报处理条目（含完整审核/上诉链）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ReportAdminVO implements Serializable {

    private Long id;

    private Long duelId;

    private String duelName;

    /**
     * 死斗状态（0 招募中 1 进行中 2 已结算 3 已解散）——已结算/已解散不可改判
     */
    private Integer duelStatus;

    /**
     * 是否互审局（决定误判留痕是否连带组长）
     */
    private Boolean peerReview;

    private String leaderName;

    private Long reporterId;

    private String reporterName;

    /**
     * 举报理由
     */
    private String reason;

    private Date createTime;

    /**
     * 状态 (0:待处理, 1:改判通过, 2:维持原判, 3:无法处理)
     */
    private Integer status;

    /**
     * 处理备注（含误判留痕）
     */
    private String handleRemark;

    private Date handleTime;

    // ===== 关联凭证与审核链 =====

    private Long evidenceId;

    private Long recordId;

    private String submitterName;

    private LocalDate checkInDate;

    private String imageUrl;

    private String previewUrl;

    /**
     * 原驳回理由
     */
    private String rejectRemark;

    /**
     * 原审核人（误判留痕对象；AI 免审/超时自动通过的驳回不存在）
     */
    private String reviewerName;

    /**
     * 该凭证的上诉记录（互审局必有：举报前提是上诉被维持；组长局为空）
     */
    private Integer appealStatus;

    private String appealReason;

    private String appealHandleRemark;

    private static final long serialVersionUID = 1L;
}
