package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 组长待仲裁上诉条目（互审局）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AppealPendingVO implements Serializable {

    private Long appealId;

    private Long recordId;

    private Long evidenceId;

    private Long userId;

    private String userName;

    private String userAvatar;

    private LocalDate checkInDate;

    /**
     * 凭证原图 URL
     */
    private String imageUrl;

    /**
     * 凭证预览图 URL
     */
    private String previewUrl;

    /**
     * 打卡时备注
     */
    private String remark;

    /**
     * 原驳回理由
     */
    private String rejectRemark;

    /**
     * 上诉理由
     */
    private String appealReason;

    /**
     * 仲裁截止时间（超时自动改判通过）
     */
    private Date deadline;

    /**
     * 上诉提交时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
