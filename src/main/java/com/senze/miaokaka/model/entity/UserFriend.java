package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 好友关系（申请-同意制；同意时写入双向两行）
 *
 * @TableName user_friend
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "user_friend")
@Data
public class UserFriend implements Serializable {

    /**
     * 记录id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起人id
     */
    private Long userId;

    /**
     * 接受人id
     */
    private Long friendId;

    /**
     * 状态 (0:待同意, 1:已同意)
     */
    private Integer status;

    /**
     * 同意时间
     */
    private Date agreeTime;

    /**
     * 申请时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
