package com.senze.miaokaka.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应：JWT + 用户信息
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
@AllArgsConstructor
public class LoginResponseVO implements Serializable {

    /**
     * JWT，后续请求放入 Authorization: Bearer {token}
     */
    private String token;

    private LoginUserVO user;

    private static final long serialVersionUID = 1L;
}
