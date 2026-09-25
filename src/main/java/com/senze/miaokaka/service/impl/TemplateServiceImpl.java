package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.UserConstant;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.PlanTemplateMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.plan.PlanCreateRequest;
import com.senze.miaokaka.model.dto.template.TemplateCreateRequest;
import com.senze.miaokaka.model.entity.PlanTemplate;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.PlanVO;
import com.senze.miaokaka.model.vo.TemplateVO;
import com.senze.miaokaka.service.CheckInPlanService;
import com.senze.miaokaka.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 计划模板服务实现。
 * 市场可见性：官方 + 全部用户公开模板；排序：官方置顶 → 使用量 → 最新。
 * 一键套用：模板字段映射为既有创建计划链路（任务清单/天数/类型复制），use_count +1。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl extends ServiceImpl<PlanTemplateMapper, PlanTemplate>
        implements TemplateService {

    private static final long MARKET_PAGE_SIZE = 20;

    private final PlanTemplateMapper planTemplateMapper;

    private final UserMapper userMapper;

    private final CheckInPlanService checkInPlanService;

    @Override
    public Page<TemplateVO> market(long current, Long viewerId) {
        Page<PlanTemplate> page = planTemplateMapper.selectPage(new Page<>(current, MARKET_PAGE_SIZE),
                new LambdaQueryWrapper<PlanTemplate>()
                        .orderByDesc(PlanTemplate::getIsOfficial)
                        .orderByDesc(PlanTemplate::getUseCount)
                        .orderByDesc(PlanTemplate::getId));
        List<PlanTemplate> rows = page.getRecords();
        Map<Long, User> creators = rows.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(rows.stream().map(PlanTemplate::getCreatorId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Page<TemplateVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(rows.stream().map(t -> {
            TemplateVO vo = new TemplateVO();
            vo.setId(t.getId());
            vo.setTemplateName(t.getTemplateName());
            vo.setTemplateDesc(t.getTemplateDesc());
            vo.setPlanType(t.getPlanType());
            vo.setTargetDays(t.getTargetDays());
            vo.setDailyTasks(parseTasks(t.getDailyTasks()));
            vo.setIsOfficial(t.getIsOfficial() != null && t.getIsOfficial() == 1);
            vo.setUseCount(t.getUseCount() == null ? 0 : t.getUseCount());
            User creator = creators.get(t.getCreatorId());
            vo.setCreatorName(creator == null ? "" : creator.getUserName());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Long create(Long userId, boolean isAdmin, TemplateCreateRequest request) {
        ThrowUtils.throwIf(request.getDailyTasks() == null || request.getDailyTasks().isEmpty(),
                ErrorCode.PARAMS_ERROR, "模板至少要有 1 项每日任务");
        PlanTemplate template = new PlanTemplate();
        template.setCreatorId(userId);
        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateDesc(StrUtil.blankToDefault(request.getTemplateDesc(), null));
        template.setPlanType(request.getPlanType() == null ? 0 : request.getPlanType());
        template.setTargetDays(request.getTargetDays() == null ? 21 : request.getTargetDays());
        List<String> tasks = request.getDailyTasks().stream().map(String::trim)
                .filter(StrUtil::isNotBlank).limit(5).toList();
        template.setDailyTasks(JSONUtil.toJsonStr(tasks));
        template.setTotalTasks(tasks.size());
        template.setIsOfficial(isAdmin ? 1 : 0);
        template.setUseCount(0);
        save(template);
        return template.getId();
    }

    @Override
    public PlanVO applyToPlan(Long userId, Long templateId) {
        PlanTemplate template = planTemplateMapper.selectById(templateId);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR, "模板不存在或已下架");
        PlanCreateRequest request = new PlanCreateRequest();
        request.setPlanName(template.getTemplateName());
        request.setPlanDesc(template.getTemplateDesc());
        request.setPlanType(template.getPlanType() == null ? 0 : template.getPlanType());
        request.setTargetDays(template.getTargetDays());
        request.setDailyTasks(parseTasks(template.getDailyTasks()));
        PlanVO plan = checkInPlanService.createPlan(userId, request);
        planTemplateMapper.update(null, new LambdaUpdateWrapper<PlanTemplate>()
                .eq(PlanTemplate::getId, templateId)
                .setSql("use_count = use_count + 1"));
        return plan;
    }

    @Override
    public void remove(Long userId, boolean isAdmin, Long templateId) {
        PlanTemplate template = planTemplateMapper.selectById(templateId);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR, "模板不存在");
        ThrowUtils.throwIf(!isAdmin && !template.getCreatorId().equals(userId),
                ErrorCode.NO_AUTH_ERROR, "仅创建者或管理员可删除模板");
        planTemplateMapper.deleteById(templateId);
    }

    private List<String> parseTasks(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            List<String> tasks = JSONUtil.toList(json, String.class);
            return tasks == null ? List.of() : tasks;
        } catch (Exception e) {
            return List.of();
        }
    }
}
