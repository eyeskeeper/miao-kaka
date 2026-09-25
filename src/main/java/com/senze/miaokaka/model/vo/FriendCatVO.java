package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 围观好友：猫列表
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FriendCatVO implements Serializable {

    private Long friendId;

    private String friendName;

    private List<CatItem> cats;

    @Data
    public static class CatItem implements Serializable {

        private Long planId;

        private String planName;

        private String catName;

        private Integer level;

        private String bossName;

        private Integer totalBossDefeated;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
