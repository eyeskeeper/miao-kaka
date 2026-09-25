package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 公告条目（admin 历史列表）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AnnouncementVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    /**
     * 发布人昵称
     */
    private String creatorName;

    /**
     * 广播送达人数
     */
    private Integer delivered;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
