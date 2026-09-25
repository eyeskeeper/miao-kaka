package com.senze.miaokaka.model.dto.mall;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 商城购买请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class MallBuyRequest implements Serializable {

    /**
     * 道具编码
     */
    @NotBlank(message = "道具编码不能为空")
    private String itemCode;

    private static final long serialVersionUID = 1L;
}
