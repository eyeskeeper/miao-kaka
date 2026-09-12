package com.senze.miaokaka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

/**
 * Redis 缓存薄封装：业务代码不直接接触 RedisTemplate。
 * 硬性原则（共识见 2026-09-11 缓存设计）：
 * 1. 涉及钱的数据绝不缓存，永远直读数据库
 * 2. Cache-Aside + 写时逐出 + TTL 兜底
 * 3. Redis 任何异常只降级为直读 DB（记 warn），绝不构成可用性故障
 *
 * 用内置 Jackson 3 JsonMapper（Boot 4 已迁移 Jackson 3，无 com.fasterxml ObjectMapper Bean 可注入；
 * Jackson 3 原生支持 java.time）。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    /**
     * 排行榜 key（全站单份）
     */
    public static final String KEY_RANK_STREAK = "miaokaka:rank:streak:top50";

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final StringRedisTemplate redisTemplate;

    @Value("${app.cache.enabled:false}")
    private boolean enabled;

    public static String keyUser(Long userId) {
        return "miaokaka:user:" + userId;
    }

    public static String keyDuelAgg(Long duelId) {
        return "miaokaka:duel:agg:" + duelId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 读缓存：未命中/开关关闭/任何异常 → 返回 null（调用方回源 DB）
     */
    public <T> T get(String key, Class<T> type) {
        if (!enabled) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return JSON_MAPPER.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存读取失败，降级回源 DB：key={}, 原因={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 写缓存：开关关闭/任何异常 → 静默跳过（下次读取回源重填）
     */
    public void put(String key, Object value, Duration ttl) {
        if (!enabled || value == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, JSON_MAPPER.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("缓存写入失败：key={}, 原因={}", key, e.getMessage());
        }
    }

    /**
     * 逐出：写路径主动调用，保证旧数据窗口收敛到毫秒级
     */
    public void evict(String... keys) {
        if (!enabled || keys == null || keys.length == 0) {
            return;
        }
        try {
            redisTemplate.delete(List.of(keys));
        } catch (Exception e) {
            log.warn("缓存逐出失败（TTL 兜底会清理）：keys={}, 原因={}", String.join(",", keys), e.getMessage());
        }
    }
}
