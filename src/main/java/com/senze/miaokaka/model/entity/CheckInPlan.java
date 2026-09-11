package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 打卡计划（连击为计划级）
 *
 * @TableName check_in_plan
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "check_in_plan")
@Data
public class CheckInPlan implements Serializable {

    /**
     * 计划id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 关联小组id（二期）
     */
    private Long teamId;

    /**
     * 引用的小组模板id（二期）
     */
    private Long templateId;

    /**
     * 计划来源 (0:个人创建, 1:小组模板, 2:死斗挑战)
     */
    private Integer planSource;

    /**
     * 计划模式 (0:普通, 1:10分钟习惯，纯标记由前端实现计时)
     */
    private Integer planMode;

    /**
     * 关联死斗id（影子计划专属）
     */
    private Long duelId;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 计划描述
     */
    private String planDesc;

    /**
     * 计划类型 (0:学习, 1:运动, 2:阅读, 3:其他)
     */
    private Integer planType;

    /**
     * 目标连续打卡天数
     */
    private Integer targetDays;

    /**
     * 提醒时间 (HH:mm，一期不推送，仅存储)
     */
    private String remindTime;

    /**
     * 每日任务列表（JSON 数组字符串，AI 草稿/用户自定义）
     */
    private String dailyTasks;

    /**
     * 当前连续打卡天数（计划级）
     */
    private Integer currentStreak;

    /**
     * 历史最长连续打卡天数（计划级）
     */
    private Integer maxStreak;

    /**
     * 总任务数
     */
    private Integer totalTasks;

    /**
     * 任务完成状态位图
     */
    private String taskProgress;

    /**
     * 已完成任务数（冗余）
     */
    private Integer completedTasks;

    /**
     * 全部完成时间
     */
    private Date completionTime;

    /**
     * 状态 (0:进行中, 1:已暂停, 2:已结束, 3:已完成)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
