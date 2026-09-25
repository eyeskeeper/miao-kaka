package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户道具背包（积分商城兑换所得；quantity 条件更新保证不为负）
 *
 * @TableName user_item
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "user_item")
@Data
public class UserItem implements Serializable {

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
     * 道具编码
     */
    private String itemCode;

    /**
     * 持有数量
     */
    private Integer quantity;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
