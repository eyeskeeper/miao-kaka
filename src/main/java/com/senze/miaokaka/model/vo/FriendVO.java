package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FriendVO implements Serializable {

    private Long userId;

    private String userName;

    private String userAvatar;

    /**
     * 全勤连击
     */
    private Integer currentStreak;

    private static final long serialVersionUID = 1L;
}
