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

    // region 以下为非缓存数据结构操作（如拍一拍收件箱/频控计数器，Redis 是唯一落点）。
    // 与缓存不同：这里 Redis 就是数据本身，故障时返回 null 由调用方拒绝服务，绝不静默丢失

    /**
     * 自增计数；首次自增或每次调用都刷新 TTL。
     *
     * @return 自增后的值；Redis 故障返回 null
     */
    public Long increment(String key, Duration ttl) {
        if (!enabled) {
            return null;
        }
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null) {
                redisTemplate.expire(key, ttl);
            }
            return value;
        } catch (Exception e) {
            log.warn("计数器操作失败：key={}, 原因={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * @return List 长度；Redis 故障返回 null
     */
    public Long listSize(String key) {
        if (!enabled) {
            return null;
        }
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.warn("List 长度查询失败：key={}, 原因={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 尾插一条消息（JSON 序列化）并刷新整个 List 的 TTL
     */
    public void listPush(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForList().rightPush(key, JSON_MAPPER.writeValueAsString(value));
            redisTemplate.expire(key, ttl);
        } catch (Exception e) {
            log.warn("List 写入失败：key={}, 原因={}", key, e.getMessage());
        }
    }

    /**
     * 读取整个 List（不删除）。
     *
     * @return 元素列表（可能为空）；Redis 故障返回 null
     */
    public <T> List<T> listRange(String key, Class<T> elementType) {
        if (!enabled) {
            return null;
        }
        try {
            List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
            if (raw == null) {
                return null;
            }
            List<T> result = new java.util.ArrayList<>(raw.size());
            for (String entry : raw) {
                try {
                    result.add(JSON_MAPPER.readValue(entry, elementType));
                } catch (Exception e) {
                    log.warn("List 元素解析失败，跳过：key={}, entry={}", key, entry);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("List 读取失败：key={}, 原因={}", key, e.getMessage());
            return null;
        }
    }

    // endregion
}
