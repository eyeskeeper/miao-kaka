package com.senze.miaokaka.model.dto.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员修改用户请求（全部可选，仅更新传入字段）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class AdminUserUpdateRequest implements Serializable {

    @Size(max = 256, message = "昵称最长 256 字")
    private String userName;

    @Size(max = 1024, message = "头像 URL 最长 1024 字")
    private String userAvatar;

    /**
     * 角色调整（仅允许 user / admin；封禁走专门接口）
     */
    @Pattern(regexp = "^(user|admin)$", message = "角色仅允许 user / admin")
    private String userRole;

    /**
     * 密码重置（明文，BCrypt 落库）
     */
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    private String newPassword;

    private static final long serialVersionUID = 1L;
}
