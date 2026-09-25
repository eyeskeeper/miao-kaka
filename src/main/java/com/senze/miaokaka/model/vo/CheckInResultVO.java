package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 打卡结果视图：事件过程 + 成长结算 + 猫口吻鼓励语
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class CheckInResultVO implements Serializable {

    /**
     * 猫精灵名
     */
    private String catName;

    /**
     * 打卡日期
     */
    private LocalDate checkInDate;

    private String remark;

    /**
     * 事件类型：attack=攻击BOSS / stat=属性提升 / crit=暴击
     */
    private String eventType;

    /**
     * 事件描述（猫做了什么）
     */
    private String eventDesc;

    /**
     * 本次对 BOSS 造成的伤害（攻击/暴击事件）
     */
    private Integer damage;

    /**
     * 击败 BOSS 时的奖励（经验/积分在此累计）
     */
    private Boolean bossDefeated;

    private String bossName;

    private Integer bossHpBefore;

    private Integer bossHpAfter;

    /**
     * 击败后出现的下一只 BOSS
     */
    private Integer newBossLevel;

    private String newBossName;

    private Integer newBossMaxHp;

    /**
     * 属性提升事件：提升的属性名（attack/defense/hp）
     */
    private String statName;

    private Integer statGain;

    /**
     * 本次获得经验
     */
    private Integer expGained;

    private Integer level;

    /**
     * 本次是否升级
     */
    private Boolean levelUp;

    /**
     * 本次获得积分
     */
    private Integer pointsEarned;

    private Integer totalPoints;

    /**
     * 打卡后的计划连击
     */
    private Integer currentStreak;

    private Integer maxStreak;

    /**
     * 猫口吻鼓励语（AI 生成，超时降级本地语录）
     */
    private String encouragement;

    /**
     * 本次新解锁的徽章名称（无则空列表）
     */
    private List<String> unlockedBadges;

    /**
     * 是否自动消耗了双倍经验卡（本次经验 ×2）
     */
    private Boolean doubleExp;

    private static final long serialVersionUID = 1L;
}
