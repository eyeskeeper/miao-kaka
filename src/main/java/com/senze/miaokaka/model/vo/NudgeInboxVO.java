package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 拍一拍收件箱（读取即消费）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class NudgeInboxVO implements Serializable {

    private Integer count;

    private List<NudgeItemVO> items;

    private static final long serialVersionUID = 1L;
}
