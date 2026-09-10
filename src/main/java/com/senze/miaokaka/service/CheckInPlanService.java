package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.dto.plan.CatRenameRequest;
import com.senze.miaokaka.model.dto.plan.PlanCreateRequest;
import com.senze.miaokaka.model.dto.plan.PlanUpdateRequest;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.vo.PlanVO;

import java.util.List;

/**
 * 打卡计划服务
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface CheckInPlanService extends IService<CheckInPlan> {

    /**
     * 创建计划并自动生成猫精灵
     */
    PlanVO createPlan(Long userId, PlanCreateRequest request);

    PlanVO updatePlan(Long userId, PlanUpdateRequest request);

    /**
     * 逻辑删除计划（猫数据保留但随计划从界面消失）
     */
    boolean deletePlan(Long userId, Long planId);

    List<PlanVO> listMyPlans(Long userId);

    PlanVO getPlanDetail(Long userId, Long planId);

    boolean renameCat(Long userId, CatRenameRequest request);

    /**
     * 校验计划存在且属于该用户，否则抛异常
     */
    CheckInPlan getOwnedPlan(Long userId, Long planId);
}
