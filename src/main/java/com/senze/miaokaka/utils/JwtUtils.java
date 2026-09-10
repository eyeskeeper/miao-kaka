package com.senze.miaokaka.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.senze.miaokaka.constant.JwtConstant;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.date.DateUtil;

/**
 * JWT 签发与解析工具（Hutool 实现，HS256）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public final class JwtUtils {

    private JwtUtils() {
    }

    /**
     * 签发 JWT
     *
     * @param userId    用户id
     * @param userRole  用户角色
     * @param secret    密钥
     * @param expireDays 有效天数
     * @return token
     */
    public static String createToken(Long userId, String userRole, String secret, int expireDays) {
        Map<String, Object> payload = new HashMap<>(4);
        payload.put(JwtConstant.CLAIM_USER_ID, userId);
        payload.put(JwtConstant.CLAIM_USER_ROLE, userRole);
        return JWT.create()
                .addPayloads(payload)
                .setExpiresAt(DateUtil.offsetDay(new Date(), expireDays))
                .setKey(secret.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    /**
     * 校验签名与有效期
     */
    public static boolean verify(String token, String secret) {
        try {
            return JWTUtil.verify(token, secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    public static Long getUserId(String token) {
        Object value = JWTUtil.parseToken(token).getPayload(JwtConstant.CLAIM_USER_ID);
        return value == null ? null : Long.valueOf(value.toString());
    }

    public static String getUserRole(String token) {
        Object value = JWTUtil.parseToken(token).getPayload(JwtConstant.CLAIM_USER_ROLE);
        return value == null ? null : value.toString();
    }

    /**
     * 从 Authorization 头提取 token（容忍无 Bearer 前缀，便于 Swagger 调试）
     */
    public static String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        String trimmed = authHeader.trim();
        if (trimmed.regionMatches(true, 0, JwtConstant.BEARER_PREFIX, 0, JwtConstant.BEARER_PREFIX.length())) {
            return trimmed.substring(JwtConstant.BEARER_PREFIX.length()).trim();
        }
        return trimmed;
    }
}
