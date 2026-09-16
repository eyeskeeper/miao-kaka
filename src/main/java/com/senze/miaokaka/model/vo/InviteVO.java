package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 我的死斗邀请（码 + 海报）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class InviteVO implements Serializable {

    /**
     * 邀请码（8位大写字母数字）
     */
    private String code;

    /**
     * 海报图片 URL（复用首张）
     */
    private String posterUrl;

    /**
     * 死斗名
     */
    private String duelName;

    /**
     * 邀请人昵称（当前调用者）
     */
    private String inviterName;

    /**
     * 二维码编码的免登录落地 URL
     */
    private String qrContent;

    private static final long serialVersionUID = 1L;
}
