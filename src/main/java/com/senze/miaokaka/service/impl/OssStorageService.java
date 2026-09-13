package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.config.OssProperties;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.service.StoredImage;
import com.senze.miaokaka.service.StorageService;
import com.senze.miaokaka.utils.ImageUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 阿里云 OSS 存储实现（`app.storage.type=oss` 时装配，Bucket 一期公共读）。
 * 原图与预览图同 key 前缀，预览图固定 `_preview.jpg` 后缀。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "oss")
@Slf4j
public class OssStorageService implements StorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final OssProperties properties;

    private final OSS ossClient;

    private final String urlPrefix;

    public OssStorageService(OssProperties properties) {
        this.properties = properties;
        ThrowIfMisconfigured(properties);
        this.ossClient = new OSSClientBuilder().build(
                properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
        this.urlPrefix = StrUtil.blankToDefault(properties.getUrlPrefix(),
                "https://" + properties.getBucket() + "." + properties.getEndpoint());
    }

    @Override
    public StoredImage storeImage(MultipartFile file) {
        validate(file);
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = StrUtil.subAfter(original, ".", true).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 jpg/jpeg/png/webp 图片");
        }
        try {
            byte[] originalBytes = file.getBytes();
            // 先压缩再上传：预览图生成失败时不产生"有原图无预览"的残缺对象
            byte[] previewBytes = ImageUtils.generatePreview(originalBytes);
            String key = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    + "/" + UUID.randomUUID().toString().replace("-", "");
            putObject(key + "." + extension, originalBytes, contentTypeOf(extension));
            putObject(key + "_preview.jpg", previewBytes, "image/jpeg");
            return new StoredImage(urlPrefix + "/" + key + "." + extension,
                    urlPrefix + "/" + key + "_preview.jpg");
        } catch (Exception e) {
            log.error("OSS 上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败，请重试");
        }
    }

    @Override
    public byte[] readAllBytes(String url) {
        String key = StrUtil.removePrefix(url, urlPrefix + "/");
        try (var object = ossClient.getObject(properties.getBucket(), key)) {
            return object.getObjectContent().readAllBytes();
        } catch (Exception e) {
            log.error("OSS 读取失败：key={}", key, e);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在或已清理");
        }
    }

    private void putObject(String key, byte[] bytes, String contentType) {
        ossClient.putObject(properties.getBucket(), key, new ByteArrayInputStream(bytes));
        // 公共读 Bucket 下对象继承桶 ACL；此处显式声明 content-type 便于浏览器直接预览
        log.debug("OSS putObject: {}/{} ({} bytes)", properties.getBucket(), key, bytes.length);
    }

    private static String contentTypeOf(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private static void ThrowIfMisconfigured(OssProperties properties) {
        if (StrUtil.hasBlank(properties.getEndpoint(), properties.getAccessKeyId(),
                properties.getAccessKeySecret(), properties.getBucket())) {
            throw new IllegalStateException(
                    "app.storage.type=oss 但 OSS 凭证不完整，请检查 ALIYUN_* 环境变量");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能超过 5MB");
        }
    }

    @PreDestroy
    public void shutdown() {
        ossClient.shutdown();
    }
}
