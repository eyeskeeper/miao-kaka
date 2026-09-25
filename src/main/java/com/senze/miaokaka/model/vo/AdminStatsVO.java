package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理端数据看板
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AdminStatsVO implements Serializable {

    /**
     * 累计注册用户（未删除）
     */
    private Integer totalUsers;

    /**
     * 今日新增用户
     */
    private Integer todayNewUsers;

    /**
     * 今日打卡人数（DAU，按打卡记录去重用户）
     */
    private Integer dauToday;

    /**
     * 昨日打卡人数
     */
    private Integer dauYesterday;

    /**
     * 今日打卡次数
     */
    private Integer checkinsToday;

    /**
     * 昨日打卡次数
     */
    private Integer checkinsYesterday;

    /**
     * 本周打卡次数（周一至今）
     */
    private Integer checkinsWeek;

    /**
     * 累计打卡次数
     */
    private Integer checkinsTotal;

    /**
     * 招募中局数
     */
    private Integer duelsRecruiting;

    /**
     * 进行中局数
     */
    private Integer duelsRunning;

    /**
     * 已结算局数
     */
    private Integer duelsSettled;

    /**
     * 近 7 天全站打卡次数趋势
     */
    private List<TrendDayVO> trend7;

    @Data
    public static class TrendDayVO implements Serializable {

        private LocalDate date;

        private Integer count;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
