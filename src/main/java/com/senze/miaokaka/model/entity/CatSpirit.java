package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 猫精灵（每计划一只猫，多猫方案）
 *
 * @TableName cat_spirit
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "cat_spirit")
@Data
public class CatSpirit implements Serializable {

    /**
     * 猫精灵id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联计划id
     */
    private Long planId;

    /**
     * 猫精灵名称
     */
    private String catName;

    /**
     * 猫精灵头像/形象
     */
    private String catAvatar;

    /**
     * 猫精灵类型 (0:勇士猫, 1:法师猫, 2:射手猫)
     */
    private Integer catType;

    /**
     * 等级
     */
    private Integer level;

    /**
     * 经验值
     */
    private Integer experience;

    /**
     * 攻击力
     */
    private Integer attack;

    /**
     * 防御力（一期展示属性，二期战斗消费）
     */
    private Integer defense;

    /**
     * 最大生命值（一期展示属性，二期战斗消费）
     */
    private Integer maxHp;

    /**
     * 当前生命值（一期展示属性，二期战斗消费）
     */
    private Integer currentHp;

    /**
     * 当前挑战的BOSS等级
     */
    private Integer bossLevel;

    /**
     * 当前BOSS名称（随机名库生成）
     */
    private String bossName;

    /**
     * 当前BOSS剩余血量（不自动回复）
     */
    private Integer bossHp;

    /**
     * 当前BOSS最大血量
     */
    private Integer bossMaxHp;

    /**
     * 累计击败BOSS数量
     */
    private Integer totalBossDefeated;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
