package com.senze.miaokaka.service;

/**
 * 一次图片存储的结果：原图 + 服务端生成的预览图
 *
 * @param url        原图访问 URL
 * @param previewUrl 预览图访问 URL
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public record StoredImage(String url, String previewUrl) {
}
