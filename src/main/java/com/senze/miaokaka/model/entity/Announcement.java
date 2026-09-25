package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统公告（admin 发布，广播复制到 user_notification）
 *
 * @TableName announcement
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "announcement")
@Data
public class Announcement implements Serializable {

    /**
     * 公告id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

    /**
     * 发布人id（admin）
     */
    private Long creatorId;

    /**
     * 发布时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
