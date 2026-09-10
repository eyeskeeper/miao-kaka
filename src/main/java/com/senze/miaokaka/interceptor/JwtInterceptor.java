package com.senze.miaokaka.interceptor;

import com.senze.miaokaka.annotation.AuthCheck;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.config.JwtProperties;
import com.senze.miaokaka.constant.JwtConstant;
import com.senze.miaokaka.constant.UserConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录态 + 角色权限拦截器
 * 每次请求回库取用户，保证封禁即时生效
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行跨域预检
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String token = JwtUtils.extractToken(request.getHeader(JwtConstant.AUTH_HEADER));
        User loginUser = null;
        if (token != null && JwtUtils.verify(token, jwtProperties.getSecret())) {
            Long userId = JwtUtils.getUserId(token);
            if (userId != null) {
                loginUser = userMapper.selectById(userId);
            }
        }
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(UserConstant.BAN_ROLE.equals(loginUser.getUserRole()),
                ErrorCode.NO_AUTH_ERROR, "账号已被封禁，请联系管理员");

        // 角色校验（方法级注解优先，其次类级）
        AuthCheck authCheck = handlerMethod.getMethodAnnotation(AuthCheck.class);
        if (authCheck == null) {
            authCheck = handlerMethod.getBeanType().getAnnotation(AuthCheck.class);
        }
        if (authCheck != null && !authCheck.mustRole().isBlank()) {
            String mustRole = authCheck.mustRole();
            if (!mustRole.equals(loginUser.getUserRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        request.setAttribute(UserConstant.USER_LOGIN_STATE, loginUser);
        return true;
    }
}
