package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户黑名单（拉黑=解除好友+封锁；静默不通知对方）
 *
 * @TableName user_block
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "user_block")
@Data
public class UserBlock implements Serializable {

    /**
     * 记录id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 拉黑人id
     */
    private Long blockerId;

    /**
     * 被拉黑人id
     */
    private Long blockedId;

    /**
     * 拉黑时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
