package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.ai.AiPlanDraftRequest;
import com.senze.miaokaka.model.vo.AiPlanDraftVO;
import com.senze.miaokaka.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 助手接口
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/ai")
@Tag(name = "AI 助手")
@Slf4j
public class AiController {

    private final ChatClient chatClient;

    @Resource
    private AiAssistantService aiAssistantService;

    public AiController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * AI 生成打卡计划草稿（草稿确认制：仅返回草稿，不落库，用户确认后调 POST /plan 创建）
     */
    @PostMapping("/plan/draft")
    @Operation(summary = "AI 生成打卡计划草稿", description = "不落库；用户确认/修改后调 POST /plan 正式创建")
    public BaseResponse<AiPlanDraftVO> planDraft(@Valid @RequestBody AiPlanDraftRequest request) {
        return ResultUtils.success(aiAssistantService.draftPlan(request.getDescription()));
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    String health() {
        return "ok";
    }
}
