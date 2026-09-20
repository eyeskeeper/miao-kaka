package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户通知（通用通知，type 区分业务）
 *
 * @TableName user_notification
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "user_notification")
@Data
public class UserNotification implements Serializable {

    /**
     * 通知id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收人id
     */
    private Long userId;

    /**
     * 通知类型 (1:被移除出死斗)
     */
    private Integer type;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

    /**
     * 关联业务id（如死斗id）
     */
    private Long refId;

    /**
     * 是否已读 (0:未读, 1:已读)
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
