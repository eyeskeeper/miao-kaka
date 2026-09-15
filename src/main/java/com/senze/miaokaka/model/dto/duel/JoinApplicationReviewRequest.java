package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 加入申请审批请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class JoinApplicationReviewRequest implements Serializable {

    /**
     * 申请id
     */
    @NotNull(message = "申请id不能为空")
    private Long requestId;

    /**
     * true=通过，false=拒绝
     */
    @NotNull(message = "审批结论不能为空")
    private Boolean approve;

    /**
     * 备注（拒绝原因）
     */
    @Size(max = 255, message = "备注最长 255 字")
    private String remark;

    private static final long serialVersionUID = 1L;
}
