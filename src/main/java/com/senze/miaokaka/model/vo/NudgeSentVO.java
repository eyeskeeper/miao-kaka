package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 拍一拍发送结果
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class NudgeSentVO implements Serializable {

    private Long toUserId;

    /**
     * 实际发出的文案
     */
    private String text;

    /**
     * 今天还剩几次可拍
     */
    private Integer dailyRemaining;

    private static final long serialVersionUID = 1L;
}
