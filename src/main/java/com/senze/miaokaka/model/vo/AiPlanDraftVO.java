package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 生成的计划草稿（不落库，用户确认/修改后调 POST /plan 创建）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AiPlanDraftVO implements Serializable {

    private String planName;

    /**
     * 0:学习, 1:运动, 2:阅读, 3:其他
     */
    private Integer planType;

    private String planDesc;

    private Integer targetDays;

    private List<String> dailyTasks;

    private static final long serialVersionUID = 1L;
}
