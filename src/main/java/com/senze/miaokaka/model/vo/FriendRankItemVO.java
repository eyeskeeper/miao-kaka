package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友排行条目（好友+我，按全勤连击排序）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FriendRankItemVO implements Serializable {

    private Long userId;

    private String userName;

    private String userAvatar;

    private Integer currentStreak;

    /**
     * 是否我
     */
    private Boolean isMe;

    private static final long serialVersionUID = 1L;
}
