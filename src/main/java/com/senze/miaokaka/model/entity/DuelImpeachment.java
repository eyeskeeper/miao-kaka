package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 死斗弹劾（组员弹劾组长投票）
 *
 * @TableName duel_impeachment
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "duel_impeachment")
@Data
public class DuelImpeachment implements Serializable {

    /**
     * 弹劾id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 死斗id
     */
    private Long duelId;

    /**
     * 发起人id（成功后成为新组长）
     */
    private Long initiatorId;

    /**
     * 弹劾原因（20字内）
     */
    private String reason;

    /**
     * 状态 (0:进行中, 1:成功, 2:失败)
     */
    private Integer status;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 弹劾票数
     */
    private Integer impeachCnt;

    /**
     * 维持票数
     */
    private Integer maintainCnt;

    /**
     * 投票截止时间（发起+24小时）
     */
    private Date expireTime;

    /**
     * 结束时间
     */
    private Date finishTime;

    /**
     * 发起时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
