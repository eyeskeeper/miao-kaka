package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 管理端用户视图
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String userAccount;

    private String userName;

    private String userAvatar;

    private String userRole;

    private Integer currentStreak;

    private Integer totalPoints;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
