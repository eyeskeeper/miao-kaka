package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 发起弹劾组长请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ImpeachInitiateRequest implements Serializable {

    /**
     * 弹劾原因（20字内）
     */
    @NotBlank(message = "弹劾原因不能为空")
    @Size(max = 20, message = "弹劾原因最长 20 字")
    private String reason;

    private static final long serialVersionUID = 1L;
}
