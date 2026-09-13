package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 打卡凭证与审核（死斗专属）
 *
 * @TableName check_in_evidence
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "check_in_evidence")
@Data
public class CheckInEvidence implements Serializable {

    /**
     * 凭证id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对应打卡记录id（唯一）
     */
    private Long recordId;

    /**
     * 死斗id
     */
    private Long duelId;

    /**
     * 打卡用户id
     */
    private Long userId;

    /**
     * 凭证图片存储路径（相对）
     */
    private String imagePath;

    /**
     * 预览图路径（服务端压缩小图，历史数据为空则回退原图）
     */
    private String imagePreviewPath;

    /**
     * 审核状态 (0:待审核, 1:通过, 2:驳回)
     */
    private Integer reviewStatus;

    /**
     * 审核人id（组长）
     */
    private Long reviewerId;

    /**
     * 是否组长自审（公示标记）
     */
    private Integer isSelfReview;

    /**
     * AI建议 (空:未启用, 0:建议通过, 1:建议驳回)
     */
    private Integer aiSuggestion;

    /**
     * AI 判断理由
     */
    private String aiReason;

    /**
     * 审核备注（驳回必填）
     */
    private String reviewRemark;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
