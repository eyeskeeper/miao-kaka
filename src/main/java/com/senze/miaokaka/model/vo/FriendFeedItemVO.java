package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 好友打卡动态条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FriendFeedItemVO implements Serializable {

    /**
     * 打卡记录id（点赞用）
     */
    private Long recordId;

    private Long friendId;

    private String friendName;

    private String friendAvatar;

    /**
     * 计划名
     */
    private String planName;

    private LocalDate checkInDate;

    /**
     * 0 正常 1 补卡
     */
    private Integer status;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 我是否已赞
     */
    private Boolean likedByMe;

    private static final long serialVersionUID = 1L;
}
