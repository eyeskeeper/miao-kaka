package com.senze.miaokaka.model.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 计划草稿请求（用户描述目标，AI 产出计划草稿，不落库）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AiPlanDraftRequest implements Serializable {

    /**
     * 目标描述，如"我想每天背 50 个单词，坚持一个月"
     */
    @NotBlank(message = "目标描述不能为空")
    @Size(max = 500, message = "目标描述最长 500 字")
    private String description;

    private static final long serialVersionUID = 1L;
}
