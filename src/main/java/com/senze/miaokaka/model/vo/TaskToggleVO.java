package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务勾选结果视图：进度 + （勾满时）自动打卡的事件结算
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class TaskToggleVO implements Serializable {

    private Long planId;

    private Integer taskIndex;

    private Boolean done;

    /**
     * 任务完成位图（如 "101"）
     */
    private String taskProgress;

    private Integer completedTasks;

    private Integer totalTasks;

    /**
     * 本次勾选后是否全部完成
     */
    private Boolean allDone;

    /**
     * 是否触发了自动打卡
     */
    private Boolean autoChecked;

    /**
     * 勾满时自动打卡的事件结算（未触发则为空）
     */
    private CheckInResultVO checkInResult;

    /**
     * 补充说明（如死斗计划需去死斗入口上传凭证）
     */
    private String message;

    private static final long serialVersionUID = 1L;
}
