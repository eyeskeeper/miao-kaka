package com.senze.miaokaka.controller;

import com.senze.miaokaka.annotation.AuthCheck;
import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.admin.AdminUserCreateRequest;
import com.senze.miaokaka.model.dto.admin.AdminUserUpdateRequest;
import com.senze.miaokaka.model.dto.admin.UserBanRequest;
import com.senze.miaokaka.model.dto.admin.UserPageQueryRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.UserVO;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端接口（仅 admin 角色）：用户增删改查 + 封禁
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/admin")
@AuthCheck(mustRole = "admin")
@Tag(name = "管理端")
@Slf4j
public class AdminController {

    @Resource
    private UserService userService;

    @PostMapping("/user")
    @Operation(summary = "建号", description = "运营手动开号，赠 1000 喵币与注册一致")
    public BaseResponse<Long> create(@Valid @RequestBody AdminUserCreateRequest request) {
        return ResultUtils.success(userService.createUser(request));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "用户详情")
    public BaseResponse<UserVO> detail(@PathVariable Long userId) {
        return ResultUtils.success(userService.getUserDetail(userId));
    }

    @PutMapping("/user/{userId}")
    @Operation(summary = "修改用户", description = "昵称/头像/角色(user↔admin)/密码重置，仅更新传入字段")
    public BaseResponse<UserVO> update(@PathVariable Long userId,
                                       @Valid @RequestBody AdminUserUpdateRequest request,
                                       HttpServletRequest servletRequest) {
        User operator = userService.getLoginUser(servletRequest);
        return ResultUtils.success(userService.updateUserDetail(operator.getId(), userId, request));
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "删除用户", description = "逻辑删除+账号归档；名下有招募中/进行中死斗押金时拒绝；封禁接口仍保留用于可逆惩罚")
    public BaseResponse<String> delete(@PathVariable Long userId, HttpServletRequest servletRequest) {
        User operator = userService.getLoginUser(servletRequest);
        userService.deleteUser(operator.getId(), userId);
        return ResultUtils.success("已删除");
    }

    @GetMapping("/user/page")
    @Operation(summary = "分页查用户", description = "支持昵称模糊搜索与角色过滤")
    public BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserVO>> page(
            UserPageQueryRequest request) {
        return ResultUtils.success(userService.pageUserVOs(request));
    }

    @PostMapping("/user/ban")
    @Operation(summary = "封禁/解封用户", description = "封禁后逐出登录态缓存，立即生效")
    public BaseResponse<Boolean> ban(@Valid @RequestBody UserBanRequest request) {
        return ResultUtils.success(userService.banUser(request));
    }
}
