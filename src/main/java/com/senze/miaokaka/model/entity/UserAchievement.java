package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户成就徽章
 *
 * @TableName user_achievement
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "user_achievement")
@Data
public class UserAchievement implements Serializable {

    /**
     * 记录id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 徽章编码
     */
    private String code;

    /**
     * 解锁时间
     */
    private Date unlockedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
