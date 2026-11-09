package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 审核记录条目（组内公示：含通过/驳回，审核人与理由可见）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ReviewHistoryVO implements Serializable {

    private Long recordId;

    /**
     * 凭证id（上诉/举报入口定位用）
     */
    private Long evidenceId;

    /**
     * 提交者
     */
    private Long userId;

    private String userName;

    private LocalDate checkInDate;

    /**
     * 审核结果 (1:通过, 2:驳回)
     */
    private Integer reviewStatus;

    /**
     * 人工审核人昵称（AI 免审/超时自动通过为空）
     */
    private String reviewerName;

    /**
     * 是否自审（公示标记）
     */
    private Boolean selfReview;

    /**
     * 互审派单审核员昵称（组内互审模式留痕）
     */
    private String assigneeName;

    /**
     * 审核备注（驳回必填）
     */
    private String reviewRemark;

    /**
     * AI建议 (空:未启用, 0:建议通过, 1:建议驳回)
     */
    private Integer aiSuggestion;

    private String aiReason;

    private Date reviewTime;

    /**
     * 该凭证的上诉状态（互审局；null=未上诉）
     */
    private Integer appealStatus;

    /**
     * 上诉理由
     */
    private String appealReason;

    /**
     * 是否已被改判（上诉/举报翻案，公示红标）
     */
    private Boolean overturned;

    private static final long serialVersionUID = 1L;
}
