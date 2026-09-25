package com.senze.miaokaka.model.dto.template;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建计划模板请求（admin 创建即官方模板）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class TemplateCreateRequest implements Serializable {

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称最长 128 字")
    private String templateName;

    /**
     * 模板描述
     */
    @Size(max = 512, message = "模板描述最长 512 字")
    private String templateDesc;

    /**
     * 计划类型 (0:学习, 1:运动, 2:阅读, 3:其他)
     */
    @Min(value = 0, message = "计划类型不合法")
    @Max(value = 3, message = "计划类型不合法")
    private Integer planType = 0;

    /**
     * 目标连续打卡天数（1~3650）
     */
    @Min(value = 1, message = "目标天数最少 1 天")
    @Max(value = 3650, message = "目标天数最多 3650 天")
    private Integer targetDays = 21;

    /**
     * 每日任务清单（≤5 项）
     */
    @Size(max = 5, message = "每日任务最多 5 项")
    private List<String> dailyTasks;

    private static final long serialVersionUID = 1L;
}
