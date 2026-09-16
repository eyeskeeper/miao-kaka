package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 死斗邀请码（每个成员每局一个固定码，海报复用）
 *
 * @TableName duel_invite
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "duel_invite")
@Data
public class DuelInvite implements Serializable {

    /**
     * 邀请id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 死斗id
     */
    private Long duelId;

    /**
     * 邀请人id（成员/组长）
     */
    private Long inviterId;

    /**
     * 邀请码（8位大写字母数字，剔除易混字符）
     */
    private String code;

    /**
     * 邀请海报 URL（首次生成后回填复用）
     */
    private String posterUrl;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
