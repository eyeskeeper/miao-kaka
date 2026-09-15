package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 死斗加入申请（审批加入模式）
 *
 * @TableName duel_join_request
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "duel_join_request")
@Data
public class DuelJoinRequest implements Serializable {

    /**
     * 申请id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 死斗id
     */
    private Long duelId;

    /**
     * 申请人id
     */
    private Long userId;

    /**
     * 状态 (0:待审核, 1:已通过, 2:已拒绝)
     */
    private Integer status;

    /**
     * 审核备注
     */
    private String reviewRemark;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 申请时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
