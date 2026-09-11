package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.senze.miaokaka.config.VisionProperties;
import com.senze.miaokaka.service.VisionReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * GLM-4V 视觉预审实现（OpenAI 兼容 chat/completions 端点，base64 图片）
 * 不用 Spring AI starter：避免与 DeepSeek 双 ChatModel 自动装配歧义，且可整体开关
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@Slf4j
public class GlmVisionReviewService implements VisionReviewService {

    private static final String REVIEW_SYSTEM_PROMPT = """
            你是打卡应用"喵卡卡"的凭证审核助手。用户参加了一个需押金的打卡挑战，每天上传一张照片作为完成凭证。
            请判断这张照片是否像一张合理的日常习惯打卡凭证（如运动、阅读、学习、家务等真实生活场景照片）。
            网图/明星/二次元/明显无关/纯文字截图应判 reject。无需苛求画质。
            只输出一个 JSON 对象，禁止任何解释或 Markdown 代码块：
            {"verdict":"pass或reject","reason":"不超过40字的理由"}
            """;

    private final VisionProperties properties;

    private final ExecutorService visionExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile RestClient restClient;

    public GlmVisionReviewService(VisionProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled() && StrUtil.isNotBlank(properties.getApiKey());
    }

    @Override
    public Optional<VisionVerdict> review(Path imagePath, String context) {
        if (!isAvailable() || !Files.exists(imagePath)) {
            return Optional.empty();
        }
        Future<Optional<VisionVerdict>> future = visionExecutor.submit(() -> doReview(imagePath, context));
        try {
            return future.get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true);
            log.warn("GLM-4V 预审超时/失败，按无 AI 意见处理：{}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<VisionVerdict> doReview(Path imagePath, String context) {
        try {
            String mime = MimeTypeUtils.parseMimeType(Files.probeContentType(imagePath)) == null
                    ? "image/jpeg"
                    : Files.probeContentType(imagePath);
            String dataUrl = "data:" + mime + ";base64,"
                    + Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", REVIEW_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", List.of(
                                    Map.of("type", "text", "text",
                                            "挑战背景：" + StrUtil.blankToDefault(context, "日常习惯打卡")),
                                    Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))))),
                    "temperature", 0.1);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient().post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            String content = extractContent(response);
            return parseVerdict(content);
        } catch (Exception e) {
            log.warn("GLM-4V 预审调用异常：{}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 懒构建（key 可能在运行期由配置刷新，构建成本可忽略）
     */
    private RestClient restClient() {
        RestClient client = restClient;
        if (client == null) {
            synchronized (this) {
                if (restClient == null) {
                    restClient = RestClient.builder()
                            .baseUrl(properties.getBaseUrl())
                            .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                            .build();
                }
                client = restClient;
            }
        }
        return client;
    }

    @SuppressWarnings("unchecked")
    private static String extractContent(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message == null ? null : (String) message.get("content");
    }

    private static Optional<VisionVerdict> parseVerdict(String content) {
        if (StrUtil.isBlank(content)) {
            return Optional.empty();
        }
        try {
            String cleaned = content.replaceAll("```+json", " ").replaceAll("```+", " ").trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return Optional.empty();
            }
            String cleaned2 = cleaned.substring(start, end + 1);
            boolean pass = cleaned2.contains("\"pass\"");
            String reason = extractJsonString(cleaned2, "reason");
            return Optional.of(new VisionVerdict(pass, reason));
        } catch (Exception e) {
            log.warn("AI 预审结果解析失败：{}", content);
            return Optional.empty();
        }
    }

    /**
     * 轻量提取 JSON 字符串字段（避免引入额外解析依赖的重量级处理）
     */
    private static String extractJsonString(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIdx + key.length());
        int open = json.indexOf('"', colon);
        int close = open < 0 ? -1 : json.indexOf('"', open + 1);
        if (open < 0 || close < 0) {
            return null;
        }
        return json.substring(open + 1, close);
    }
}
