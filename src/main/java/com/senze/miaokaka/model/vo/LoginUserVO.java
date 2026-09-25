package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录用户视图（脱敏，不含密码）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class LoginUserVO implements Serializable {

    private Long id;

    private String userAccount;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    /**
     * 全勤连击天数
     */
    private Integer currentStreak;

    /**
     * 可用积分
     */
    private Integer totalPoints;

    /**
     * 小鱼干（好友点赞获得）
     */
    private Integer driedFish;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
