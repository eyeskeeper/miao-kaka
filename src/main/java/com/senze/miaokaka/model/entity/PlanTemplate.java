package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 计划模板（模板市场：官方/用户公开模板，一键套用建计划）
 *
 * @TableName plan_template
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "plan_template")
@Data
public class PlanTemplate implements Serializable {

    /**
     * 模板id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建者id（admin 创建即官方模板）
     */
    private Long creatorId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String templateDesc;

    /**
     * 计划类型 (0:学习, 1:运动, 2:阅读, 3:其他)
     */
    private Integer planType;

    /**
     * 目标连续打卡天数
     */
    private Integer targetDays;

    /**
     * 每日任务配置 (JSON数组)
     */
    private String dailyTasks;

    /**
     * 总任务数
     */
    private Integer totalTasks;

    /**
     * 是否官方模板 (0:否, 1:是)
     */
    private Integer isOfficial;

    /**
     * 被套用次数
     */
    private Integer useCount;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
