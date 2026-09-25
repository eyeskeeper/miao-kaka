package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 模板市场条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class TemplateVO implements Serializable {

    private Long id;

    private String templateName;

    private String templateDesc;

    /**
     * 计划类型 (0:学习, 1:运动, 2:阅读, 3:其他)
     */
    private Integer planType;

    private Integer targetDays;

    /**
     * 每日任务清单
     */
    private List<String> dailyTasks;

    /**
     * 是否官方模板
     */
    private Boolean isOfficial;

    /**
     * 创建者昵称
     */
    private String creatorName;

    /**
     * 被套用次数
     */
    private Integer useCount;

    private static final long serialVersionUID = 1L;
}
