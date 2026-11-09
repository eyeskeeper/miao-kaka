package com.senze.miaokaka.model.dto.plan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建打卡计划请求（创建时自动生成猫精灵）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class PlanCreateRequest implements Serializable {

    /**
     * 计划名称
     */
    @NotBlank(message = "计划名称不能为空")
    @Size(max = 128, message = "计划名称最长 128 字")
    private String planName;

    /**
     * 计划描述
     */
    @Size(max = 512, message = "计划描述最长 512 字")
    private String planDesc;

    /**
     * 计划类型 (0:学习, 1:运动, 2:阅读, 3:其他)
     */
    @Min(value = 0, message = "计划类型不合法")
    @Max(value = 3, message = "计划类型不合法")
    private Integer planType = 0;

    /**
     * 目标连续打卡天数
     */
    @Min(value = 0, message = "目标天数不能为负")
    @Max(value = 3650, message = "目标天数过大")
    private Integer targetDays = 0;

    /**
     * 提醒时间 HH:mm（一期仅存储不推送）
     */
    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "提醒时间格式应为 HH:mm")
    private String remindTime;

    /**
     * 每日任务列表（可选，1~5 项；10 分钟习惯模式忽略清单）
     */
    @Size(max = 5, message = "每日任务最多 5 项")
    private List<String> dailyTasks;

    /**
     * 计划模式 (0:普通打卡, 1:10分钟习惯——计时由前端本地实现，后端只做标记)；默认 0
     */
    @Min(value = 0, message = "计划模式不合法")
    @Max(value = 1, message = "计划模式不合法")
    private Integer planMode = 0;

    private static final long serialVersionUID = 1L;
}
