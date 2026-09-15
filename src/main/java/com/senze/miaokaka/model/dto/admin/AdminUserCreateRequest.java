package com.senze.miaokaka.model.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员建号请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AdminUserCreateRequest implements Serializable {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度需在 4~32 位之间")
    private String userAccount;

    /**
     * 初始密码（BCrypt 落库）
     */
    @NotBlank(message = "初始密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    private String initialPassword;

    /**
     * 昵称（可选，默认随机喵友号）
     */
    @Size(max = 256, message = "昵称最长 256 字")
    private String userName;

    private static final long serialVersionUID = 1L;
}
