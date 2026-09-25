package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 徽章墙条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AchievementVO implements Serializable {

    /**
     * 徽章编码
     */
    private String code;

    /**
     * 徽章名称
     */
    private String name;

    /**
     * 解锁条件文案
     */
    private String description;

    /**
     * 是否已解锁
     */
    private Boolean unlocked;

    /**
     * 解锁时间（未解锁为 null）
     */
    private Date unlockedAt;

    private static final long serialVersionUID = 1L;
}
