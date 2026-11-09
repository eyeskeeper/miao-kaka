package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 提交凭证上诉请求（互审局驳回后 48h 内，每凭证限一次）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AppealSubmitRequest implements Serializable {

    /**
     * 凭证id
     */
    @NotNull(message = "凭证id不能为空")
    private Long evidenceId;

    /**
     * 上诉理由
     */
    @NotBlank(message = "上诉理由不能为空")
    @Size(max = 255, message = "上诉理由最长 255 字")
    private String reason;

    private static final long serialVersionUID = 1L;
}
