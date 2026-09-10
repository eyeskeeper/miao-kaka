package com.senze.miaokaka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（生产环境务必通过环境变量覆盖 jwt.secret）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    /**
     * 签名密钥
     */
    private String secret;

    /**
     * 有效期（天）
     */
    private int expireDays = 7;
}
