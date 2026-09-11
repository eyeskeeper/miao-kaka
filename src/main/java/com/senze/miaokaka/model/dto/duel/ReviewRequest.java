package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 死斗凭证审核请求（驳回时 reviewRemark 必填，服务层校验）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ReviewRequest implements Serializable {

    /**
     * 打卡记录id
     */
    @NotNull(message = "打卡记录id不能为空")
    private Long recordId;

    /**
     * true=通过，false=驳回
     */
    @NotNull(message = "审核结论不能为空")
    private Boolean approve;

    /**
     * 审核备注（驳回必填）
     */
    @Size(max = 255, message = "审核备注最长 255 字")
    private String reviewRemark;

    private static final long serialVersionUID = 1L;
}
