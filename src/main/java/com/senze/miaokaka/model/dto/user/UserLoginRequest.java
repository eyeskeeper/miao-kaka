package com.senze.miaokaka.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class UserLoginRequest implements Serializable {

    @NotBlank(message = "账号不能为空")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    private String userPassword;

    private static final long serialVersionUID = 1L;
}
