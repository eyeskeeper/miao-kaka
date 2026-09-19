package com.senze.miaokaka.model.dto.duel;

import com.senze.miaokaka.common.PageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 死斗大厅查询请求（全部条件可选，缺省即全量招募中+进行中）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DuelHallQueryRequest extends PageRequest {

    /**
     * 死斗名称（模糊匹配）
     */
    @Size(max = 30, message = "名称关键字最长 30 字")
    private String duelName;

    /**
     * 类型 (0:自由加入, 1:审批加入)；空=不限
     */
    @Min(value = 0, message = "类型不合法")
    @Max(value = 1, message = "类型不合法")
    private Integer joinMode;

    /**
     * 状态 (0:招募中, 1:进行中)；空=不限
     */
    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "状态不合法")
    private Integer status;

    private static final long serialVersionUID = 1L;
}
