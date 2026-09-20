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
     * 每日任务清单（JSON 数组字符串，AI 草稿/组长自定义；空=无任务清单）
     */
    private String dailyTasks;

    /**
     * 组长id（创建者）
     */
    private Long leaderId;

    /**
     * 加入模式 (0:自由加入, 1:审批加入)
     */
    private Integer joinMode;

    /**
     * 是否隐藏 (0:公开, 1:隐藏；隐藏局不在招募大厅出现)
     */
    private Integer hidden;

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
     * 人数上限（2~50，创建时组长确定；存量局默认 50）
     */
    private Integer maxMembers;

    /**
     * 当前奖池总额（人数×押金，冗余）
     */
    private Integer totalPool;

    /**
     * 被移除成员罚没池（结算时按剩余成员天数占比分配）
     */
    private Integer removedPool;

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
