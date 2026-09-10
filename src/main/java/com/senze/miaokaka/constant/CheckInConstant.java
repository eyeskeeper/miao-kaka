package com.senze.miaokaka.constant;

import java.time.ZoneId;

/**
 * 打卡/计划业务常量
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface CheckInConstant {

    /**
     * 业务时区：所有"一天"按北京时间切日
     */
    ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    // region 计划状态

    int PLAN_STATUS_ACTIVE = 0;
    int PLAN_STATUS_PAUSED = 1;
    int PLAN_STATUS_FINISHED = 2;
    int PLAN_STATUS_COMPLETED = 3;

    // endregion

    // region 打卡记录状态

    int RECORD_STATUS_NORMAL = 0;
    int RECORD_STATUS_MAKEUP = 1;

    // endregion

    // region 事件类型（对外返回值）

    String EVENT_TYPE_ATTACK = "attack";
    String EVENT_TYPE_STAT = "stat";
    String EVENT_TYPE_CRIT = "crit";

    // endregion
}
