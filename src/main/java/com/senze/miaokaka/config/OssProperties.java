package com.senze.miaokaka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置（app.storage.type=oss 时启用；凭证一律走环境变量）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Component
@ConfigurationProperties(prefix = "app.storage.oss")
@Data
public class OssProperties {

    /**
     * 地域端点，如 oss-cn-beijing.aliyuncs.com
     */
    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    /**
     * Bucket 名（一期公共读）
     */
    private String bucket;

    /**
     * 对象访问 URL 前缀，默认 https://{bucket}.{endpoint}
     */
    private String urlPrefix;
}
