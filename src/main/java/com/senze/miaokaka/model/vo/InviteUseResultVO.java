package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 使用邀请码的结果
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class InviteUseResultVO implements Serializable {

    /**
     * joined=已直接入组 / applied=已提交加入申请待组长审批
     */
    private String action;

    private DuelVO duel;

    private static final long serialVersionUID = 1L;
}
