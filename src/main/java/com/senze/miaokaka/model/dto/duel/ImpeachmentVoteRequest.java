package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 弹劾投票请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ImpeachmentVoteRequest implements Serializable {

    /**
     * 弹劾id
     */
    @NotNull(message = "弹劾id不能为空")
    private Long impeachmentId;

    /**
     * 投票结论 (0:维持, 1:弹劾)
     */
    @NotNull(message = "投票结论不能为空")
    @Min(value = 0, message = "投票结论不合法")
    @Max(value = 1, message = "投票结论不合法")
    private Integer vote;

    private static final long serialVersionUID = 1L;
}
