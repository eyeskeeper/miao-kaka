package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 一键通过加入申请的结果统计
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ApplicationsApproveAllVO implements Serializable {

    /**
     * 通过数（已扣押金入组）
     */
    private Integer approved = 0;

    /**
     * 自动拒绝数（申请人喵币不足，已留痕）
     */
    private Integer rejected = 0;

    /**
     * 满员跳过数（保持待审，名额释放后组长可再批）
     */
    private Integer skipped = 0;

    private static final long serialVersionUID = 1L;
}
