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
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * JWT 登录态 + 角色权限拦截器
 * 用户对象走 Redis 缓存（TTL 5 分钟，写路径逐出），封禁经管理端逐出后毫秒级生效
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    private final UserMapper userMapper;

    private final CacheService cacheService;

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
                loginUser = loadUser(userId);
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

    /**
     * 登录态用户缓存旁路：命中省一次回库；缓存对象不含密码摘要
     */
    private User loadUser(Long userId) {
        String key = CacheService.keyUser(userId);
        User user = cacheService.get(key, User.class);
        if (user != null) {
            return user;
        }
        user = userMapper.selectById(userId);
        if (user != null) {
            user.setUserPassword(null);
            cacheService.put(key, user, Duration.ofMinutes(5));
        }
        return user;
    }
}
