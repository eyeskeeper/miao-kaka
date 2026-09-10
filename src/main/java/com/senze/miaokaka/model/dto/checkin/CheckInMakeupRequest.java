package com.senze.miaokaka.model.dto.checkin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 补卡请求（扣积分，自然月限 2 次，恢复连击）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class CheckInMakeupRequest implements Serializable {

    /**
     * 计划id
     */
    @NotNull(message = "计划id不能为空")
    private Long planId;

    /**
     * 补卡日期 yyyy-MM-dd（只能是今天之前的日期）
     */
    @NotNull(message = "补卡日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式应为 yyyy-MM-dd")
    private String date;

    private static final long serialVersionUID = 1L;
}
