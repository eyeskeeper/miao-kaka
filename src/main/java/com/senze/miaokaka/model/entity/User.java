package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户
 *
 * @TableName user
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "user")
@Data
public class User implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码（BCrypt 摘要）
     */
    private String userPassword;

    /**
     * 微信开放平台id（二期）
     */
    private String unionId;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 公众号openid（二期）
     */
    private String mpOpenId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    /**
     * 全勤连击天数（当天全部进行中计划都完成才累计）
     */
    private Integer currentStreak;

    /**
     * 当前可用积分余额
     */
    private Integer totalPoints;

    /**
     * 喵币余额（押金货币，注册赠送1000）
     */
    private Integer miaoCoins;

    /**
     * 拍一拍模板文案（空则用系统默认）
     */
    private String nudgeText;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
