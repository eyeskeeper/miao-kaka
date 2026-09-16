package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 使用邀请码请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class InviteUseRequest implements Serializable {

    /**
     * 邀请码
     */
    @NotBlank(message = "邀请码不能为空")
    private String code;

    private static final long serialVersionUID = 1L;
}
