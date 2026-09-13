package com.senze.miaokaka.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象：一期本地磁盘（LocalStorageService），配置 `app.storage.type=oss` 时
 * 切换阿里云 OSS（OssStorageService）。两者都负责服务端压缩生成预览图。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface StorageService {

    /**
     * 存储图片：原图 + 服务端压缩的预览图，返回两个可公开访问的 URL
     */
    StoredImage storeImage(MultipartFile file);

    /**
     * 读取已存储图片的字节（AI 预审用）
     */
    byte[] readAllBytes(String url);
}
