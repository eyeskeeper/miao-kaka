package com.senze.miaokaka.utils;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * 图片压缩工具：服务端生成预览图（单一事实来源，规格服务端说了算）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public final class ImageUtils {

    /**
     * 预览图最长边（px）
     */
    public static final int PREVIEW_MAX_SIZE = 750;

    /**
     * JPEG 压缩质量
     */
    public static final double PREVIEW_QUALITY = 0.7;

    private ImageUtils() {
    }

    /**
     * 从 URL/文件名推断图片 MIME（供 AI 预审构造 data URL）
     */
    public static String mimeOf(String url) {
        String lower = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    /**
     * 生成预览图：最长边压到 750px、JPEG 质量 0.7、透明背景转白底（PNG→JPEG 缩体积）
     *
     * @param original 原图字节
     * @return JPEG 预览图字节
     */
    public static byte[] generatePreview(byte[] original) throws IOException {
        BufferedImage source = ImageIO.read(new java.io.ByteArrayInputStream(original));
        if (source == null) {
            throw new IOException("无法解析图片内容");
        }
        // 透明通道转白底（JPEG 无透明，默认填黑很难看）
        BufferedImage canvas = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.drawImage(source, 0, 0, Color.WHITE, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(canvas)
                .size(PREVIEW_MAX_SIZE, PREVIEW_MAX_SIZE)
                .outputQuality(PREVIEW_QUALITY)
                .outputFormat("jpeg")
                .toOutputStream(out);
        return out.toByteArray();
    }
}
