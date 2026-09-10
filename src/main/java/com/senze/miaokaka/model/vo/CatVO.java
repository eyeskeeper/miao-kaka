package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 猫精灵视图（完整属性 + BOSS 血条）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class CatVO implements Serializable {

    private Long id;

    private Long planId;

    private String catName;

    private String catAvatar;

    /**
     * 0:勇士猫, 1:法师猫, 2:射手猫
     */
    private Integer catType;

    private Integer level;

    private Integer experience;

    /**
     * 升级所需经验（= 50 × level^1.5）
     */
    private Integer expToNextLevel;

    private Integer attack;

    private Integer defense;

    private Integer maxHp;

    private Integer currentHp;

    private Integer bossLevel;

    private String bossName;

    private Integer bossHp;

    private Integer bossMaxHp;

    private Integer totalBossDefeated;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
