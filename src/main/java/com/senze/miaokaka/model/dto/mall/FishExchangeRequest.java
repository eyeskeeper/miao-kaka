package com.senze.miaokaka.model.dto.mall;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 小鱼干兑换积分请求（1:1）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FishExchangeRequest implements Serializable {

    /**
     * 兑换的小鱼干数量（1:1 兑积分）
     */
    @NotNull(message = "小鱼干数量不能为空")
    @Min(value = 1, message = "至少兑换 1 条小鱼干")
    @Max(value = 999, message = "单次最多兑换 999 条")
    private Integer fish;

    private static final long serialVersionUID = 1L;
}
