package com.senze.miaokaka.model.dto.checkin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 打卡记录查询请求（按计划+月份查日历）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class CheckInQueryRequest implements Serializable {

    /**
     * 计划id
     */
    @NotNull(message = "计划id不能为空")
    private Long planId;

    /**
     * 月份 yyyy-MM（不传则查当月）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 yyyy-MM")
    private String month;

    private static final long serialVersionUID = 1L;
}
