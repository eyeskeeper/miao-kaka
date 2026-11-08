package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 招募期修改死斗设置请求（仅审核相关，开赛后锁定）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class DuelUpdateRequest implements Serializable {

    /**
     * 审核方式 (0:组长审核, 1:组内互审)；不传不改
     */
    @Min(value = 0, message = "审核方式不合法")
    @Max(value = 1, message = "审核方式不合法")
    private Integer reviewMode;

    /**
     * 是否开启 AI 预审；不传不改
     */
    private Boolean aiReview;

    private static final long serialVersionUID = 1L;
}
