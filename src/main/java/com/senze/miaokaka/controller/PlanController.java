package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.DeleteRequest;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.plan.CatRenameRequest;
import com.senze.miaokaka.model.dto.plan.PlanCreateRequest;
import com.senze.miaokaka.model.dto.plan.PlanUpdateRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.PlanVO;
import com.senze.miaokaka.service.CheckInPlanService;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 打卡计划接口（创建计划自动生成猫精灵）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/plan")
@Tag(name = "打卡计划")
@Slf4j
public class PlanController {

    @Resource
    private CheckInPlanService checkInPlanService;

    @Resource
    private UserService userService;

    @PostMapping
    @Operation(summary = "创建计划", description = "创建成功自动领养一只随机类型的猫精灵")
    public BaseResponse<PlanVO> create(@Valid @RequestBody PlanCreateRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInPlanService.createPlan(user.getId(), request));
    }

    @PostMapping("/update")
    @Operation(summary = "更新计划", description = "仅允许在 进行中(0)/已暂停(1) 间切换状态")
    public BaseResponse<PlanVO> update(@Valid @RequestBody PlanUpdateRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInPlanService.updatePlan(user.getId(), request));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除计划", description = "逻辑删除，猫精灵数据保留但随计划从界面消失")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInPlanService.deletePlan(user.getId(), request.getId()));
    }

    @GetMapping("/list")
    @Operation(summary = "我的计划列表", description = "含连击、猫摘要、今日是否已打卡")
    public BaseResponse<List<PlanVO>> list(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInPlanService.listMyPlans(user.getId()));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "计划详情", description = "含猫完整属性与 BOSS 血条")
    public BaseResponse<PlanVO> detail(@PathVariable Long planId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInPlanService.getPlanDetail(user.getId(), planId));
    }

    @PostMapping("/cat/name")
    @Operation(summary = "猫精灵改名")
    public BaseResponse<Boolean> renameCat(@Valid @RequestBody CatRenameRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInPlanService.renameCat(user.getId(), request));
    }
}
