package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * 本地磁盘存储实现：日期分目录 + UUID 文件名
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final String uploadDir;

    public LocalStorageService(@Value("${app.upload-dir:./data/uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public String storeImage(MultipartFile file) {
        ThrowIfInvalid(file);
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = StrUtil.subAfter(original, ".", true).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 jpg/jpeg/png/webp 图片");
        }
        String relative = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Path target = Paths.get(uploadDir, relative);
            Files.createDirectories(target.getParent());
            file.transferTo(target.toAbsolutePath());
        } catch (IOException e) {
            log.error("保存上传图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败，请重试");
        }
        return "/uploads/" + relative;
    }

    /**
     * 读取已存储图片的本地绝对路径（AI 预审用）
     */
    public Path resolve(String relativeUrl) {
        String relative = StrUtil.removePrefix(relativeUrl, "/uploads/");
        return Paths.get(uploadDir, relative).toAbsolutePath();
    }

    private static void ThrowIfInvalid(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能超过 5MB");
        }
    }
}
