package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 喵币流水（余额只随流水变动，balance_after 可重放对账）
 *
 * @TableName coin_transaction
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "coin_transaction")
@Data
public class CoinTransaction implements Serializable {

    /**
     * 流水id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 类型 (0:注册赠送, 1:押金支出, 2:退还, 3:奖池分得, 4:管理员调整)
     */
    private Integer type;

    /**
     * 变动数额（正收入/负支出）
     */
    private Integer amount;

    /**
     * 变动后余额
     */
    private Integer balanceAfter;

    /**
     * 关联业务id（死斗id）
     */
    private Long bizId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
