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
 * 打卡记录（按计划粒度，(user_id, plan_id, check_in_date) 唯一）
 *
 * @TableName check_in_record
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "check_in_record")
@Data
public class CheckInRecord implements Serializable {

    /**
     * 打卡记录id
     */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /**
     * 关联用户id
     */
    private Long userId;

    /**
     * 关联打卡计划id
     */
    private Long planId;

    /**
     * 打卡日期（北京时间）
     */
    private LocalDate checkInDate;

    /**
     * 具体打卡时间点
     */
    private Date checkInTime;

    /**
     * 打卡状态 (0:正常, 1:补卡, 2:异常)
     */
    private Integer status;

    /**
     * 打卡备注
     */
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
