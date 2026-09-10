package com.senze.miaokaka.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class UserRegisterRequest implements Serializable {

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度需在 4~32 位之间")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    private String userPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}
