package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 死斗弹劾投票（一人一票，不可改票）
 *
 * @TableName duel_impeachment_vote
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "duel_impeachment_vote")
@Data
public class DuelImpeachmentVote implements Serializable {

    /**
     * 投票id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 弹劾id
     */
    private Long impeachmentId;

    /**
     * 死斗id
     */
    private Long duelId;

    /**
     * 投票人id
     */
    private Long userId;

    /**
     * 投票 (0:维持, 1:弹劾)
     */
    private Integer vote;

    /**
     * 投票时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
