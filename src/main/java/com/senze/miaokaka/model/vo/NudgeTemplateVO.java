package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 拍一拍模板视图
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class NudgeTemplateVO implements Serializable {

    /**
     * 已存模板（可能为空）
     */
    private String nudgeText;

    /**
     * 实际会发出的文案（模板为空时为系统默认）
     */
    private String effectiveText;

    private static final long serialVersionUID = 1L;
}
