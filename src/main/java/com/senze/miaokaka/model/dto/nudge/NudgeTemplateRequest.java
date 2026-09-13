package com.senze.miaokaka.model.dto.nudge;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 拍一拍模板编辑请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class NudgeTemplateRequest implements Serializable {

    /**
     * 模板文案（≤20 字；空串=清除模板，拍一拍时使用系统默认）
     */
    @Size(max = 20, message = "拍一拍文案最长 20 字")
    private String nudgeText;

    private static final long serialVersionUID = 1L;
}
