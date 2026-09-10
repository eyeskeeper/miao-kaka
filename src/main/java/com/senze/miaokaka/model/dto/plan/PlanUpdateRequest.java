package com.senze.miaokaka.model.dto.plan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新打卡计划请求（仅允许 0:进行中 / 1:已暂停 两种状态切换）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class PlanUpdateRequest implements Serializable {

    /**
     * 计划id
     */
    @NotNull(message = "计划id不能为空")
    private Long id;

    @Size(max = 128, message = "计划名称最长 128 字")
    private String planName;

    @Size(max = 512, message = "计划描述最长 512 字")
    private String planDesc;

    @Min(value = 0, message = "计划类型不合法")
    @Max(value = 3, message = "计划类型不合法")
    private Integer planType;

    @Min(value = 0, message = "目标天数不能为负")
    @Max(value = 3650, message = "目标天数过大")
    private Integer targetDays;

    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "提醒时间格式应为 HH:mm")
    private String remindTime;

    /**
     * 目标状态（仅允许 0:进行中 / 1:已暂停）
     */
    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "仅允许进行中/已暂停")
    private Integer status;

    private static final long serialVersionUID = 1L;
}
