package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 组长仲裁上诉请求（维持驳回 / 改判通过）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AppealResolveRequest implements Serializable {

    /**
     * 上诉id
     */
    @NotNull(message = "上诉id不能为空")
    private Long appealId;

    /**
     * true=改判通过，false=维持驳回
     */
    @NotNull(message = "仲裁结论不能为空")
    private Boolean approve;

    /**
     * 仲裁备注（给上诉人的说明）
     */
    @Size(max = 255, message = "备注最长 255 字")
    private String remark;

    private static final long serialVersionUID = 1L;
}
