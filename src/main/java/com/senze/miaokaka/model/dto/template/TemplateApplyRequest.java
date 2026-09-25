package com.senze.miaokaka.model.dto.template;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 从模板一键建计划请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class TemplateApplyRequest implements Serializable {

    /**
     * 模板id
     */
    @NotNull(message = "模板id不能为空")
    private Long templateId;

    private static final long serialVersionUID = 1L;
}
