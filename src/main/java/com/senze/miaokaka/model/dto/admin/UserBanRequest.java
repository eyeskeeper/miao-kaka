package com.senze.miaokaka.model.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 封禁/解封用户请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class UserBanRequest implements Serializable {

    /**
     * 目标用户id
     */
    @NotNull(message = "用户id不能为空")
    private Long userId;

    /**
     * true=封禁，false=解封
     */
    @NotNull(message = "操作类型不能为空")
    private Boolean isBan;

    private static final long serialVersionUID = 1L;
}
