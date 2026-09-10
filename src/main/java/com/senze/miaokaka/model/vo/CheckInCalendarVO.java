package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 打卡日历视图（某计划某月）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class CheckInCalendarVO implements Serializable {

    private Long planId;

    /**
     * yyyy-MM
     */
    private String month;

    /**
     * 该月有打卡记录的日期及状态
     */
    private List<DayRecord> days;

    @Data
    public static class DayRecord implements Serializable {

        private LocalDate date;

        /**
         * 0:正常, 1:补卡
         */
        private Integer status;

        private String remark;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
