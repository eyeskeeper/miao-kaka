package com.senze.miaokaka.service;

import java.util.Optional;

/**
 * 视觉预审服务：对打卡凭证给出建议结论（不决定结果）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface VisionReviewService {

    /**
     * 是否可用（开关开启且已配置 key）
     */
    boolean isAvailable();

    /**
     * 审图。返回空 = 未启用/调用失败（调用方按无 AI 意见处理，永不抛出）
     *
     * @param imageBytes 图片字节
     * @param mime       图片 MIME 类型
     * @param context    打卡上下文描述（死斗名称等）
     */
    Optional<VisionVerdict> review(byte[] imageBytes, String mime, String context);

    /**
     * AI 结论
     */
    record VisionVerdict(boolean suggestApprove, String reason) {
    }
}
