package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 背包道具条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class BagItemVO implements Serializable {

    private String code;

    private String name;

    private Integer quantity;

    private static final long serialVersionUID = 1L;
}
