package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 商城商品条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class MallItemVO implements Serializable {

    /**
     * 道具编码
     */
    private String code;

    /**
     * 商品名
     */
    private String name;

    /**
     * 价格（积分）
     */
    private Integer price;

    /**
     * 效果描述
     */
    private String description;

    /**
     * 我当前持有数量
     */
    private Integer owned;

    private static final long serialVersionUID = 1L;
}
