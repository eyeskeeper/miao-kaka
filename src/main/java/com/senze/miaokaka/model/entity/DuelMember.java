package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 死斗成员
 *
 * @TableName duel_member
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "duel_member")
@Data
public class DuelMember implements Serializable {

    /**
     * 成员id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 死斗id
     */
    private Long duelId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 影子计划id（复用打卡引擎）
     */
    private Long planId;

    /**
     * 本人押金（喵币快照）
     */
    private Integer deposit;

    /**
     * 已确认打卡天数（结算时重算）
     */
    private Integer checkinDays;

    /**
     * 状态 (0:已加入, 1:进行中, 2:已结算, 3:已退出)
     */
    private Integer status;

    /**
     * 加入时间
     */
    private Date joinTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
