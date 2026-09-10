package com.senze.miaokaka.service;

import com.senze.miaokaka.model.vo.AiPlanDraftVO;

/**
 * AI 助手服务（DeepSeek）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface AiAssistantService {

    /**
     * 用户描述目标 → AI 产出计划草稿（不落库，用户确认后走统一创建接口）
     */
    AiPlanDraftVO draftPlan(String description);

    /**
     * 打卡事件后生成一句猫口吻鼓励语；超时/异常降级为本地语录库，永不抛出
     */
    String generateEncouragement(String catName, String eventDesc);
}
