package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 统计热力图单日数据
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class HeatmapDayVO implements Serializable {

    private LocalDate date;

    /**
     * 当日打卡次数（正常+补卡）
     */
    private Integer count;

    private static final long serialVersionUID = 1L;
}
