package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 一条拍一拍消息
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class NudgeItemVO implements Serializable {

    private Long fromUserId;

    private String fromUserName;

    private String text;

    private LocalDateTime time;

    private static final long serialVersionUID = 1L;
}
