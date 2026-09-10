package com.senze.miaokaka.model.dto.admin;

import com.senze.miaokaka.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 管理员分页查用户请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQueryRequest extends PageRequest implements Serializable {

    /**
     * 昵称模糊搜索（可选）
     */
    private String userName;

    /**
     * 角色过滤（可选）
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
