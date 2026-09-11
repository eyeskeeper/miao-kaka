package com.senze.miaokaka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 视觉预审（智谱 GLM-4V，OpenAI 兼容端点）配置
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Component
@ConfigurationProperties(prefix = "app.ai.vision")
@Data
public class VisionProperties {

    /**
     * 总开关：关闭时死斗凭证审核退化为纯组长人工
     */
    private boolean enabled = false;

    /**
     * OpenAI 兼容端点根地址
     */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /**
     * API Key（生产走环境变量）
     */
    private String apiKey = "";

    /**
     * 模型名
     */
    private String model = "glm-4v-flash";

    /**
     * 单次预审超时秒数
     */
    private int timeoutSeconds = 8;
}
