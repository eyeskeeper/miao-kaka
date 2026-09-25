package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.senze.miaokaka.aiAssitant.constant.prompt.SchemeDesignPromptConstant;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.NameLibraryConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.model.vo.AiPlanDraftVO;
import com.senze.miaokaka.service.AiAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * AI 助手服务实现
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@Slf4j
public class AiAssistantServiceImpl implements AiAssistantService {

    /**
     * 鼓励语生成超时：打卡是主流程，AI 慢了就降级，不能拖死接口
     */
    private static final long ENCOURAGE_TIMEOUT_SECONDS = 4;

    /**
     * 周报总结超时：非主流程关键路径，稍长
     */
    private static final long WEEKLY_TIMEOUT_SECONDS = 8;

    /**
     * 虚拟线程执行 AI 调用（Java 21），主线程限时等待
     */
    private final ExecutorService aiExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final ChatClient chatClient;

    public AiAssistantServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public AiPlanDraftVO draftPlan(String description) {
        String content;
        try {
            content = chatClient.prompt()
                    .system(SchemeDesignPromptConstant.PLAN_DRAFT_SYSTEM_PROMPT)
                    .user(description)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 生成计划草稿调用失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 助手暂时不可用，请稍后重试或手动创建计划");
        }
        JSONObject json = parsePlanJson(content);
        return toDraftVO(json);
    }

    @Override
    public String generateEncouragement(String catName, String eventDesc) {
        String userMessage = String.format("猫精灵名：%s%n刚发生的打卡事件：%s", catName, eventDesc);
        Future<String> future = aiExecutor.submit(() -> chatClient.prompt()
                .system(SchemeDesignPromptConstant.CAT_ENCOURAGE_SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content());
        try {
            String text = future.get(ENCOURAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return StrUtil.isBlank(text) ? NameLibraryConstant.randomFallbackEncouragement() : text.trim();
        } catch (Exception e) {
            future.cancel(true);
            log.warn("AI 鼓励语生成超时/失败，降级为本地语录：{}", e.getMessage());
            return NameLibraryConstant.randomFallbackEncouragement();
        }
    }

    @Override
    public String generateWeeklySummary(String statsSummary) {
        if (StrUtil.isBlank(statsSummary)) {
            return "";
        }
        Future<String> future = aiExecutor.submit(() -> chatClient.prompt()
                .system(SchemeDesignPromptConstant.WEEKLY_SUMMARY_SYSTEM_PROMPT)
                .user(statsSummary)
                .call()
                .content());
        try {
            String text = future.get(WEEKLY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            future.cancel(true);
            log.warn("AI 周报总结生成超时/失败，降级为模板文案：{}", e.getMessage());
            return "";
        }
    }

    /**
     * 解析 AI 输出的 JSON（容忍 Markdown 代码块包裹与前后杂文本）
     */
    private JSONObject parsePlanJson(String content) {
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.SYSTEM_ERROR, "AI 没有返回内容，请重试或手动创建");
        String cleaned = content.replaceAll("```+json", " ").replaceAll("```+", " ").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        ThrowUtils.throwIf(start < 0 || end <= start, ErrorCode.SYSTEM_ERROR, "AI 生成的计划无法解析，请重试或手动创建");
        try {
            return JSONUtil.parseObj(cleaned.substring(start, end + 1));
        } catch (Exception e) {
            log.error("AI 计划草稿 JSON 解析失败，原文：{}", content, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成的计划无法解析，请重试或手动创建");
        }
    }

    /**
     * 草稿字段清洗与钳制：AI 输出不可信，缺省给安全默认值
     */
    private AiPlanDraftVO toDraftVO(JSONObject json) {
        AiPlanDraftVO vo = new AiPlanDraftVO();
        String name = StrUtil.trimToEmpty(json.getStr("planName", ""));
        vo.setPlanName(name.isEmpty() ? "我的打卡计划" : StrUtil.sub(name, 0, 128));
        int planType = json.getInt("planType", 3);
        vo.setPlanType(Math.min(Math.max(planType, 0), 3));
        vo.setPlanDesc(StrUtil.sub(StrUtil.trimToEmpty(json.getStr("planDesc", "")), 0, 512));
        int targetDays = json.getInt("targetDays", 21);
        vo.setTargetDays(Math.min(Math.max(targetDays, 1), 365));
        List<String> tasks = json.getBeanList("dailyTasks", String.class) == null
                ? List.of()
                : json.getBeanList("dailyTasks", String.class).stream()
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .limit(5)
                        .toList();
        vo.setDailyTasks(tasks);
        return vo;
    }
}
