package com.senze.miaokaka.service;

import cn.hutool.core.util.StrUtil;
import com.senze.miaokaka.utils.ImageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 凭证 AI 预审扩展点（预留方法）：
 * 图片存储成功的那一刻预审，结论写 Redis（key 挂 URL，TTL 7 天）；
 * 死斗凭证提交时直接消费已存结论，不再二次调用 AI。
 * 开关关闭（`app.ai.vision.enabled=false` 或未配 key）时全链路为空操作，
 * 未来打开开关即自动生效，业务代码无需再改。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceAiPreCheckService {

    private static final String VERDICT_KEY_PREFIX = "miaokaka:evidence:ai:";

    private static final Duration VERDICT_TTL = Duration.ofDays(7);

    private final VisionReviewService visionReviewService;

    private final StorageService storageService;

    private final CacheService cacheService;

    /**
     * 上传成功后调用：AI 可用且结论未缓存时执行一次预审（结果进 Redis）。
     * 任何失败只记日志，绝不影响上传主流程。
     */
    public void preCheck(String imageUrl) {
        if (!visionReviewService.isAvailable()) {
            return;
        }
        String key = verdictKey(imageUrl);
        if (cacheService.get(key, AiVerdict.class) != null) {
            return;
        }
        try {
            byte[] bytes = storageService.readAllBytes(imageUrl);
            visionReviewService.review(bytes, ImageUtils.mimeOf(imageUrl), "死斗打卡凭证")
                    .ifPresent(verdict -> {
                        AiVerdict ai = new AiVerdict(verdict.suggestApprove(), verdict.reason());
                        cacheService.put(key, ai, VERDICT_TTL);
                        log.info("凭证 AI 预审完成：url={}, 建议={}, 理由={}", imageUrl,
                                ai.suggestApprove() ? "通过" : "驳回", ai.reason());
                    });
        } catch (Exception e) {
            log.warn("凭证 AI 预审失败（不影响上传）：url={}, 原因={}", imageUrl, e.getMessage());
        }
    }

    /**
     * 消费已缓存的预审结论（非破坏性读取，TTL 到期自动清理）
     */
    public Optional<AiVerdict> consume(String imageUrl) {
        if (!visionReviewService.isAvailable()) {
            return Optional.empty();
        }
        return Optional.ofNullable(cacheService.get(verdictKey(imageUrl), AiVerdict.class));
    }

    public record AiVerdict(boolean suggestApprove, String reason) {
    }

    private static String verdictKey(String imageUrl) {
        return VERDICT_KEY_PREFIX + cn.hutool.crypto.digest.DigestUtil.md5Hex(
                StrUtil.blankToDefault(imageUrl, ""));
    }
}
