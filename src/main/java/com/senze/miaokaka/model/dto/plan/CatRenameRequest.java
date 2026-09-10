package com.senze.miaokaka.model.dto.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 猫精灵改名请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class CatRenameRequest implements Serializable {

    /**
     * 计划id（猫跟随计划）
     */
    @NotNull(message = "计划id不能为空")
    private Long planId;

    /**
     * 新名字
     */
    @NotBlank(message = "猫名不能为空")
    @Size(max = 64, message = "猫名最长 64 字")
    private String catName;

    private static final long serialVersionUID = 1L;
}
