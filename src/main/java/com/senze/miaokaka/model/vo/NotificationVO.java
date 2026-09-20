package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户通知条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class NotificationVO implements Serializable {

    private Long id;

    /**
     * 通知类型 (1:被移除出死斗)
     */
    private Integer type;

    private String title;

    private String content;

    /**
     * 关联业务id（如死斗id，前端可跳转）
     */
    private Long refId;

    /**
     * 是否已读
     */
    private Boolean isRead;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
