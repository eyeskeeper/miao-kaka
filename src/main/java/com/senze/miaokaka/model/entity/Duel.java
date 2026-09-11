package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 习惯死斗挑战
 *
 * @TableName duel
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "duel")
@Data
public class Duel implements Serializable {

    /**
     * 死斗id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 死斗名称
     */
    private String duelName;

    /**
     * 死斗描述
     */
    private String duelDesc;

    /**
     * 组长id（创建者）
     */
    private Long leaderId;

    /**
     * 每人押金（喵币）
     */
    private Integer depositPerMember;

    /**
     * 挑战天数
     */
    private Integer totalDays;

    /**
     * 开始日期（北京时间）
     */
    private LocalDate startDate;

    /**
     * 结束日期 = 开始日期 + total_days - 1
     */
    private LocalDate endDate;

    /**
     * 状态 (0:招募中, 1:进行中, 2:已结算, 3:已解散)
     */
    private Integer status;

    /**
     * 当前人数（冗余）
     */
    private Integer memberCount;

    /**
     * 当前奖池总额（人数×押金，冗余）
     */
    private Integer totalPool;

    /**
     * 结算是否完成（幂等标记）
     */
    private Integer settled;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
