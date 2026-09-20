package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.CheckInPlanMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.model.dto.plan.CatRenameRequest;
import com.senze.miaokaka.model.dto.plan.PlanCreateRequest;
import com.senze.miaokaka.model.dto.plan.PlanUpdateRequest;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.CatSpirit;
import com.senze.miaokaka.model.vo.CatVO;
import com.senze.miaokaka.model.vo.PlanVO;
import com.senze.miaokaka.service.CatSpiritService;
import com.senze.miaokaka.service.CheckInPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 打卡计划服务实现
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
public class CheckInPlanServiceImpl extends ServiceImpl<CheckInPlanMapper, CheckInPlan>
        implements CheckInPlanService {

    private final CatSpiritService catSpiritService;

    private final CheckInRecordMapper checkInRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanVO createPlan(Long userId, PlanCreateRequest request) {
        CheckInPlan plan = new CheckInPlan();
        plan.setUserId(userId);
        plan.setPlanSource(0);
        plan.setPlanName(request.getPlanName().trim());
        plan.setPlanDesc(StrUtil.blankToDefault(request.getPlanDesc(), null));
        plan.setPlanType(request.getPlanType() == null ? 0 : request.getPlanType());
        plan.setTargetDays(request.getTargetDays() == null ? 0 : request.getTargetDays());
        plan.setRemindTime(StrUtil.isBlank(request.getRemindTime()) ? null : request.getRemindTime());
        plan.setDailyTasks(toDailyTasksJson(request.getDailyTasks()));
        plan.setCurrentStreak(0);
        plan.setMaxStreak(0);
        plan.setTotalTasks(request.getDailyTasks() == null ? 1 : request.getDailyTasks().size());
        plan.setTaskProgress("");
        plan.setCompletedTasks(0);
        plan.setStatus(CheckInConstant.PLAN_STATUS_ACTIVE);
        boolean saved = save(plan);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "创建计划失败，请重试");
        // 每计划一只猫：创建计划即领养
        catSpiritService.createCatForPlan(plan.getId());
        // 回读以填充数据库默认值（create_time 等）
        return getPlanDetail(userId, plan.getId());
    }

    @Override
    public PlanVO updatePlan(Long userId, PlanUpdateRequest request) {
        CheckInPlan plan = getOwnedPlan(userId, request.getId());
        // 影子计划通道封闭：状态由死斗流程管理，禁止手动暂停/恢复
        ThrowUtils.throwIf(isShadowPlan(plan) && request.getStatus() != null && !request.getStatus().equals(plan.getStatus()),
                ErrorCode.OPERATION_ERROR, "死斗计划状态由死斗流程管理，不可手动切换");
        if (StrUtil.isNotBlank(request.getPlanName())) {
            plan.setPlanName(request.getPlanName().trim());
        }
        if (request.getPlanDesc() != null) {
            plan.setPlanDesc(StrUtil.isBlank(request.getPlanDesc()) ? null : request.getPlanDesc());
        }
        if (request.getPlanType() != null) {
            plan.setPlanType(request.getPlanType());
        }
        if (request.getTargetDays() != null) {
            plan.setTargetDays(request.getTargetDays());
        }
        if (request.getRemindTime() != null) {
            plan.setRemindTime(StrUtil.isBlank(request.getRemindTime()) ? null : request.getRemindTime());
        }
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }
        boolean updated = updateById(plan);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新计划失败");
        return getPlanDetail(userId, plan.getId());
    }

    @Override
    public boolean deletePlan(Long userId, Long planId) {
        CheckInPlan plan = getOwnedPlan(userId, planId);
        // 影子计划通道封闭：删除死斗计划等于销毁押金凭证，禁止
        ThrowUtils.throwIf(isShadowPlan(plan), ErrorCode.OPERATION_ERROR, "死斗计划不可删除，由死斗流程管理");
        return removeById(plan.getId());
    }

    @Override
    public List<PlanVO> listMyPlans(Long userId) {
        List<CheckInPlan> plans = list(new LambdaQueryWrapper<CheckInPlan>()
                .eq(CheckInPlan::getUserId, userId)
                .orderByDesc(CheckInPlan::getCreateTime));
        if (plans.isEmpty()) {
            return List.of();
        }
        Set<Long> checkedToday = findPlanIdsCheckedToday(userId,
                plans.stream().map(CheckInPlan::getId).collect(Collectors.toSet()));
        Map<Long, CatSpirit> catByPlan = catSpiritService.list(new LambdaQueryWrapper<CatSpirit>()
                        .in(CatSpirit::getPlanId, plans.stream().map(CheckInPlan::getId).toList()))
                .stream()
                .collect(Collectors.toMap(CatSpirit::getPlanId, Function.identity()));
        return plans.stream()
                .map(plan -> toPlanVO(plan, catByPlan.get(plan.getId()), checkedToday.contains(plan.getId())))
                .toList();
    }

    @Override
    public PlanVO getPlanDetail(Long userId, Long planId) {
        CheckInPlan plan = getOwnedPlan(userId, planId);
        boolean checkedToday = findPlanIdsCheckedToday(userId, Set.of(planId)).contains(planId);
        return buildPlanVO(plan, checkedToday);
    }

    @Override
    public boolean renameCat(Long userId, CatRenameRequest request) {
        CheckInPlan plan = getOwnedPlan(userId, request.getPlanId());
        CatSpirit cat = catSpiritService.getByPlanId(plan.getId());
        ThrowUtils.throwIf(cat == null, ErrorCode.NOT_FOUND_ERROR, "猫精灵不存在");
        return catSpiritService.rename(cat.getId(), request.getCatName().trim());
    }

    @Override
    public CheckInPlan getOwnedPlan(Long userId, Long planId) {
        CheckInPlan plan = getById(planId);
        ThrowUtils.throwIf(plan == null, ErrorCode.NOT_FOUND_ERROR, "计划不存在");
        ThrowUtils.throwIf(!plan.getUserId().equals(userId), ErrorCode.FORBIDDEN_ERROR, "无权操作该计划");
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckInPlan createShadowPlan(Long userId, String duelName, int totalDays, Long duelId,
                                        java.util.List<String> dailyTasks) {
        CheckInPlan plan = new CheckInPlan();
        plan.setUserId(userId);
        plan.setPlanSource(CheckInConstant.PLAN_SOURCE_DUEL);
        plan.setPlanMode(0);
        plan.setDuelId(duelId);
        plan.setPlanName(duelName);
        plan.setPlanType(3);
        plan.setTargetDays(totalDays);
        plan.setCurrentStreak(0);
        plan.setMaxStreak(0);
        if (dailyTasks != null && !dailyTasks.isEmpty()) {
            // 复制死斗任务清单：成员可勾选；勾满不自动打卡，凭证审核仍是唯一完成门槛
            plan.setDailyTasks(JSONUtil.toJsonStr(dailyTasks));
            plan.setTotalTasks(dailyTasks.size());
        } else {
            plan.setTotalTasks(1);
        }
        plan.setTaskProgress("");
        plan.setCompletedTasks(0);
        plan.setStatus(CheckInConstant.PLAN_STATUS_ACTIVE);
        boolean saved = save(plan);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "影子计划创建失败");
        catSpiritService.createCatForPlan(plan.getId());
        return plan;
    }

    private boolean isShadowPlan(CheckInPlan plan) {
        return plan.getPlanSource() != null && plan.getPlanSource() == CheckInConstant.PLAN_SOURCE_DUEL;
    }

    private PlanVO buildPlanVO(CheckInPlan plan, boolean checkedToday) {
        CatSpirit cat = catSpiritService.getByPlanId(plan.getId());
        return toPlanVO(plan, cat, checkedToday);
    }

    private PlanVO toPlanVO(CheckInPlan plan, CatSpirit cat, boolean checkedToday) {
        PlanVO vo = new PlanVO();
        vo.setId(plan.getId());
        vo.setPlanName(plan.getPlanName());
        vo.setPlanDesc(plan.getPlanDesc());
        vo.setPlanType(plan.getPlanType());
        vo.setTargetDays(plan.getTargetDays());
        vo.setRemindTime(plan.getRemindTime());
        vo.setDailyTasks(fromDailyTasksJson(plan.getDailyTasks()));
        vo.setStatus(plan.getStatus());
        vo.setCurrentStreak(plan.getCurrentStreak());
        vo.setMaxStreak(plan.getMaxStreak());
        vo.setTodayChecked(checkedToday);
        vo.setCat(cat == null ? null : catSpiritService.toCatVO(cat));
        vo.setCreateTime(plan.getCreateTime());
        return vo;
    }

    private Set<Long> findPlanIdsCheckedToday(Long userId, Set<Long> planIds) {
        if (planIds.isEmpty()) {
            return Set.of();
        }
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        return checkInRecordMapper.selectList(new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getUserId, userId)
                        .in(CheckInRecord::getPlanId, planIds)
                        .eq(CheckInRecord::getCheckInDate, today))
                .stream()
                .map(CheckInRecord::getPlanId)
                .collect(Collectors.toSet());
    }

    private String toDailyTasksJson(List<String> dailyTasks) {
        if (dailyTasks == null || dailyTasks.isEmpty()) {
            return null;
        }
        List<String> tasks = dailyTasks.stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .limit(5)
                .toList();
        return tasks.isEmpty() ? null : JSONUtil.toJsonStr(tasks);
    }

    private List<String> fromDailyTasksJson(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
