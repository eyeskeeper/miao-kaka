package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 本周打卡统计周报（周一 ~ 今天/周日）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class WeeklyStatsVO implements Serializable {

    private LocalDate weekStart;

    private LocalDate weekEnd;

    /**
     * 本周每日打卡次数（仅截至今天）
     */
    private List<HeatmapDayVO> perDay;

    /**
     * 每个进行中计划的本周完成天数
     */
    private List<PlanWeekVO> perPlan;

    /**
     * 本周总打卡次数
     */
    private Integer totalCheckins;

    /**
     * 上周总打卡次数（对比用）
     */
    private Integer lastWeekTotal;

    /**
     * 全勤连击（user.current_streak）
     */
    private Integer currentFullStreak;

    /**
     * 最强计划连击（进行中计划里最大的 max_streak）
     */
    private Integer bestPlanStreak;

    /**
     * AI 周报总结（DeepSeek 失败时为模板文案）
     */
    private String aiSummary;

    @Data
    public static class PlanWeekVO implements Serializable {

        private Long planId;

        private String planName;

        /**
         * 本周已完成天数（正常+补卡）
         */
        private Integer days;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
