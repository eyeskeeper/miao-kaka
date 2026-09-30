package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 黑名单条目
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class BlockedUserVO implements Serializable {

    /**
     * 被拉黑用户id
     */
    private Long userId;

    private String userName;

    private String userAccount;

    private String userAvatar;

    /**
     * 拉黑时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
