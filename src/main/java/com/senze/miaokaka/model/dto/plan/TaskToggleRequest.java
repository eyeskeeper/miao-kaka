package com.senze.miaokaka.model.dto.plan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 每日任务勾选请求（0 基索引）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class TaskToggleRequest implements Serializable {

    /**
     * 任务下标（0 基，对应每日任务列表顺序）
     */
    @NotNull(message = "任务下标不能为空")
    @Min(value = 0, message = "任务下标不合法")
    @Max(value = 49, message = "任务下标超出范围")
    private Integer taskIndex;

    /**
     * true=完成，false=取消完成
     */
    @NotNull(message = "勾选状态不能为空")
    private Boolean done;

    private static final long serialVersionUID = 1L;
}
