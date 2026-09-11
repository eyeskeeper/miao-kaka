package com.senze.miaokaka.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象（一期本地磁盘，二期可换 OSS 实现类）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface StorageService {

    /**
     * 存储图片，返回可公开访问的相对 URL（如 /uploads/2026/09/11/uuid.jpg）
     */
    String storeImage(MultipartFile file);

    /**
     * 相对 URL → 本地文件路径（AI 预审读取用）
     */
    java.nio.file.Path resolve(String url);
}
