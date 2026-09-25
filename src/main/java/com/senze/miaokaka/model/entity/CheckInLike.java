package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 打卡点赞（好友点赞打卡记录，被赞者得小鱼干）
 *
 * @TableName check_in_like
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "check_in_like")
@Data
public class CheckInLike implements Serializable {

    /**
     * 点赞id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被赞打卡记录id
     */
    private Long recordId;

    /**
     * 点赞人id
     */
    private Long likerId;

    /**
     * 记录主人id（冗余）
     */
    private Long targetId;

    /**
     * 点赞日期（日限统计）
     */
    private LocalDate likeDate;

    /**
     * 点赞时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
