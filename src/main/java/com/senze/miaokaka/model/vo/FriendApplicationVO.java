package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 收到的好友申请条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FriendApplicationVO implements Serializable {

    /**
     * 申请记录id（同意/拒绝用）
     */
    private Long id;

    private Long userId;

    private String userName;

    private String userAvatar;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
