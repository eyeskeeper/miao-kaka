package com.senze.miaokaka.utils;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 邀请海报渲染器（单套模板）：暖色底 + 死斗信息 + 二维码，输出 PNG 字节。
 * 中文渲染依赖系统字体（逻辑字体 SansSerif 自动映射平台中文字体）；
 * Linux 容器部署需安装中文字体，否则文字会变豆腐块。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public final class PosterRenderer {

    public static final int WIDTH = 750;

    public static final int HEIGHT = 1000;

    private static final int QR_SIZE = 340;

    private static final Color BG_COLOR = new Color(255, 248, 236);

    private static final Color ACCENT_COLOR = new Color(255, 159, 67);

    private static final Color TITLE_COLOR = new Color(140, 90, 43);

    private static final Color MAIN_COLOR = new Color(61, 43, 31);

    private static final Color INFO_COLOR = new Color(107, 79, 51);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 30);

    private static final Font NAME_FONT = new Font("SansSerif", Font.BOLD, 44);

    private static final Font INFO_FONT = new Font("SansSerif", Font.PLAIN, 30);

    private static final Font HINT_FONT = new Font("SansSerif", Font.PLAIN, 26);

    private PosterRenderer() {
    }

    /**
     * 渲染死斗邀请海报
     *
     * @param qrContent 二维码内容（免登录落地 URL）
     */
    public static byte[] renderDuelInvite(String qrContent, String duelName, String leaderName,
                                          String inviterName, int deposit, int totalDays, int memberCount)
            throws IOException {
        QrConfig qrConfig = new QrConfig(QR_SIZE, QR_SIZE);
        qrConfig.setMargin(1);
        BufferedImage qrImage = QrCodeUtil.generate(qrContent, qrConfig);

        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 背景 + 上下橙色饰条
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(ACCENT_COLOR);
        g.fillRect(0, 0, WIDTH, 14);
        g.fillRect(0, HEIGHT - 14, WIDTH, 14);

        // 顶部标签与死斗名
        g.setColor(TITLE_COLOR);
        drawCentered(g, TITLE_FONT, "喵卡卡 · 习惯死斗", 78);
        g.setColor(MAIN_COLOR);
        drawCentered(g, NAME_FONT, truncate(g, NAME_FONT, duelName, WIDTH - 120), 152);

        // 信息区
        g.setColor(INFO_COLOR);
        int y = 232;
        y = drawLine(g, "组　长：" + truncate(g, INFO_FONT, leaderName, WIDTH - 300), y);
        y = drawLine(g, "押　金：" + deposit + " 喵币", y);
        y = drawLine(g, "周　期：" + totalDays + " 天", y);
        y = drawLine(g, "已加入：" + memberCount + " 人", y);

        // 邀请语 + 二维码（白色衬底）
        g.setColor(TITLE_COLOR);
        drawCentered(g, TITLE_FONT, "「" + truncate(g, TITLE_FONT, inviterName, 320) + "」邀请你加入", 452);
        int qrX = (WIDTH - QR_SIZE) / 2;
        int qrY = 500;
        g.setColor(Color.WHITE);
        g.fillRect(qrX - 18, qrY - 18, QR_SIZE + 36, QR_SIZE + 36);
        g.setStroke(new BasicStroke(2));
        g.setColor(ACCENT_COLOR);
        g.drawRect(qrX - 18, qrY - 18, QR_SIZE + 36, QR_SIZE + 36);
        g.drawImage(qrImage, qrX, qrY, QR_SIZE, QR_SIZE, null);

        // 底部提示
        g.setColor(TITLE_COLOR);
        drawCentered(g, HINT_FONT, "打开喵卡卡扫一扫，或输入邀请码加入", 920);
        g.dispose();
        return toPng(canvas);
    }

    private static int drawLine(Graphics2D g, String text, int y) {
        g.setFont(INFO_FONT);
        g.drawString(text, 120, y);
        return y + 54;
    }

    private static void drawCentered(Graphics2D g, Font font, String text, int baselineY) {
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);
        int x = (WIDTH - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, baselineY);
    }

    private static String truncate(Graphics2D g, Font font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        FontMetrics metrics = g.getFontMetrics(font);
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsised = text + "…";
        while (ellipsised.length() > 2 && metrics.stringWidth(ellipsised) > maxWidth) {
            ellipsised = ellipsised.substring(0, ellipsised.length() - 2) + "…";
        }
        return ellipsised;
    }

    private static byte[] toPng(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}
