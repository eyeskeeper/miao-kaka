package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 补卡结果视图
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class MakeupResultVO implements Serializable {

    /**
     * 补卡的日期
     */
    private LocalDate checkInDate;

    /**
     * 本次消耗积分
     */
    private Integer pointsCost;

    /**
     * 补卡后剩余积分
     */
    private Integer totalPoints;

    /**
     * 补卡后计划连击（可能因补上缺口而恢复）
     */
    private Integer currentStreak;

    private Integer maxStreak;

    private static final long serialVersionUID = 1L;
}
