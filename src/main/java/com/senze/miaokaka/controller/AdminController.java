package com.senze.miaokaka.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.senze.miaokaka.annotation.AuthCheck;
import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.admin.UserBanRequest;
import com.senze.miaokaka.model.dto.admin.UserPageQueryRequest;
import com.senze.miaokaka.model.vo.UserVO;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端接口（仅 admin 角色）
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

    @GetMapping("/user/page")
    @Operation(summary = "分页查用户", description = "支持昵称模糊搜索与角色过滤")
    public BaseResponse<Page<UserVO>> page(UserPageQueryRequest request) {
        return ResultUtils.success(userService.pageUserVOs(request));
    }

    @PostMapping("/user/ban")
    @Operation(summary = "封禁/解封用户", description = "封禁后该用户 JWT 立即失效（拦截器回库校验）")
    public BaseResponse<Boolean> ban(@Valid @RequestBody UserBanRequest request) {
        return ResultUtils.success(userService.banUser(request));
    }
}
