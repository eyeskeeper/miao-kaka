package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 提交死斗举报请求（组长局驳回直报；互审局上诉维持后可报；每人每局限一次）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ReportSubmitRequest implements Serializable {

    /**
     * 关联凭证id
     */
    @NotNull(message = "凭证id不能为空")
    private Long evidenceId;

    /**
     * 举报理由
     */
    @NotBlank(message = "举报理由不能为空")
    @Size(max = 255, message = "举报理由最长 255 字")
    private String reason;

    private static final long serialVersionUID = 1L;
}
