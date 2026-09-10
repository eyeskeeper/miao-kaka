package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 全勤连击排行榜条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class RankItemVO implements Serializable {

    /**
     * 名次（1 起）
     */
    private Integer rank;

    private Long userId;

    private String userName;

    private String userAvatar;

    /**
     * 全勤连击天数
     */
    private Integer currentStreak;

    private static final long serialVersionUID = 1L;
}
