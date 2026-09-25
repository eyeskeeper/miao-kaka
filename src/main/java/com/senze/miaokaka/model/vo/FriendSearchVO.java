package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友搜索结果（加好友前定位用户）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FriendSearchVO implements Serializable {

    private Long userId;

    private String userAccount;

    private String userName;

    private String userAvatar;

    private static final long serialVersionUID = 1L;
}
