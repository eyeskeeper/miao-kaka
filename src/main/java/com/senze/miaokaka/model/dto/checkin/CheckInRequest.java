package com.senze.miaokaka.model.dto.checkin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 打卡请求（按计划粒度，每天每计划一次）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class CheckInRequest implements Serializable {

    /**
     * 计划id
     */
    @NotNull(message = "计划id不能为空")
    private Long planId;

    /**
     * 打卡备注（可选）
     */
    @Size(max = 255, message = "备注最长 255 字")
    private String remark;

    private static final long serialVersionUID = 1L;
}
