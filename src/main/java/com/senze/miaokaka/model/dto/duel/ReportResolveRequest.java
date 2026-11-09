package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * admin 处理举报请求（改判通过 / 维持原判）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ReportResolveRequest implements Serializable {

    /**
     * true=改判通过，false=维持原判
     */
    @NotNull(message = "处理结论不能为空")
    private Boolean approve;

    /**
     * 处理备注（含误判留痕）
     */
    @Size(max = 255, message = "备注最长 255 字")
    private String remark;

    private static final long serialVersionUID = 1L;
}
