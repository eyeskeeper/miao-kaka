package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 商城购买结果
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class MallBuyResultVO implements Serializable {

    /**
     * 购买后剩余积分
     */
    private Integer totalPoints;

    /**
     * 购买后该道具持有数量
     */
    private Integer owned;

    private static final long serialVersionUID = 1L;
}
