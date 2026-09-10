package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.user.UserLoginRequest;
import com.senze.miaokaka.model.dto.user.UserRegisterRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.LoginResponseVO;
import com.senze.miaokaka.model.vo.LoginUserVO;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "注册", description = "账号 4~32 位，密码 8~32 位")
    public BaseResponse<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResultUtils.success(userService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "返回 JWT，后续请求放 Authorization: Bearer {token}")
    public BaseResponse<LoginResponseVO> login(@Valid @RequestBody UserLoginRequest request) {
        return ResultUtils.success(userService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息")
    public BaseResponse<LoginUserVO> me(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.toLoginUserVO(user));
    }
}
