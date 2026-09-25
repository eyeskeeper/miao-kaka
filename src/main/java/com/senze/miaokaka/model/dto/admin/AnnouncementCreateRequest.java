package com.senze.miaokaka.model.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 发布公告请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AnnouncementCreateRequest implements Serializable {

    /**
     * 标题
     */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 64, message = "公告标题最长 64 字")
    private String title;

    /**
     * 正文
     */
    @NotBlank(message = "公告内容不能为空")
    @Size(max = 512, message = "公告内容最长 512 字")
    private String content;

    private static final long serialVersionUID = 1L;
}
