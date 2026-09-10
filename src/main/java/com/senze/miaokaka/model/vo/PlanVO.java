package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 打卡计划视图（含猫与今日打卡状态）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class PlanVO implements Serializable {

    private Long id;

    private String planName;

    private String planDesc;

    /**
     * 0:学习, 1:运动, 2:阅读, 3:其他
     */
    private Integer planType;

    private Integer targetDays;

    private String remindTime;

    /**
     * 每日任务列表
     */
    private List<String> dailyTasks;

    /**
     * 状态 (0:进行中, 1:已暂停, 2:已结束, 3:已完成)
     */
    private Integer status;

    /**
     * 当前连续打卡天数（计划级）
     */
    private Integer currentStreak;

    /**
     * 历史最长连击
     */
    private Integer maxStreak;

    /**
     * 今天是否已打卡（北京时间）
     */
    private Boolean todayChecked;

    /**
     * 绑定的猫精灵
     */
    private CatVO cat;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
