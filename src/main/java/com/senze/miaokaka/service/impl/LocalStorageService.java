package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.service.StoredImage;
import com.senze.miaokaka.service.StorageService;
import com.senze.miaokaka.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘存储实现（默认）：日期分目录 + UUID 文件名，原图与预览图同目录
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageService implements StorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final String uploadDir;

    public LocalStorageService(@Value("${app.upload-dir:./data/uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
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
            // 先压缩再落盘：预览图生成失败时不留下"有原图无预览"的残缺状态
            byte[] previewBytes = ImageUtils.generatePreview(originalBytes);
            String relative = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    + "/" + UUID.randomUUID().toString().replace("-", "");
            Path originalTarget = Paths.get(uploadDir, relative + "." + extension);
            Files.createDirectories(originalTarget.getParent());
            Files.write(originalTarget, originalBytes);
            Path previewTarget = Paths.get(uploadDir, relative + "_preview.jpg");
            Files.write(previewTarget, previewBytes);
            return new StoredImage("/uploads/" + relative + "." + extension,
                    "/uploads/" + relative + "_preview.jpg");
        } catch (IOException e) {
            log.error("保存上传图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败，请重试");
        }
    }

    @Override
    public String storeImage(byte[] bytes, String ext) {
        try {
            String relative = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = Paths.get(uploadDir, relative);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return "/uploads/" + relative;
        } catch (IOException e) {
            log.error("保存生成图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败，请重试");
        }
    }

    @Override
    public byte[] readAllBytes(String url) {
        try {
            String relative = StrUtil.removePrefix(url, "/uploads/");
            return Files.readAllBytes(Paths.get(uploadDir, relative).toAbsolutePath());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在或已清理");
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
}
