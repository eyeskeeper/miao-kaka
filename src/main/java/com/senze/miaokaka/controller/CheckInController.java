package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.checkin.CheckInMakeupRequest;
import com.senze.miaokaka.model.dto.checkin.CheckInRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.AchievementVO;
import com.senze.miaokaka.model.vo.CheckInCalendarVO;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.MakeupResultVO;
import com.senze.miaokaka.service.AchievementService;
import com.senze.miaokaka.service.CheckInRecordService;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 打卡接口
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/check_in")
@Tag(name = "打卡")
@Slf4j
public class CheckInController {

    @Resource
    private CheckInRecordService checkInRecordService;

    @Resource
    private AchievementService achievementService;

    @Resource
    private UserService userService;

    @PostMapping
    @Operation(summary = "打卡", description = "每计划每天一次；触发随机事件（攻击BOSS/属性提升/暴击），返回事件过程、成长结算与猫口吻鼓励语")
    public BaseResponse<CheckInResultVO> checkIn(@Valid @RequestBody CheckInRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInRecordService.checkIn(user.getId(), request.getPlanId(), request.getRemark()));
    }

    @PostMapping("/makeup")
    @Operation(summary = "补卡", description = "扣 50 积分（可选用补卡券免扣），自然月限 2 次，仅能补最近 30 天内、计划创建后的日期；不触发事件")
    public BaseResponse<MakeupResultVO> makeup(@Valid @RequestBody CheckInMakeupRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInRecordService.makeup(user.getId(), request.getPlanId(), request.getDate(),
                Boolean.TRUE.equals(request.getUseVoucher())));
    }

    @GetMapping("/records")
    @Operation(summary = "打卡日历", description = "按计划+月份查询，month 格式 yyyy-MM，缺省当月")
    public BaseResponse<CheckInCalendarVO> records(@RequestParam Long planId,
                                                   @RequestParam(required = false) String month,
                                                   HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInRecordService.calendar(user.getId(), planId, month));
    }

    @GetMapping("/achievements")
    @Operation(summary = "徽章墙", description = "全部成就徽章 + 我的解锁状态/时间")
    public BaseResponse<List<AchievementVO>> achievements(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(achievementService.wall(user.getId()));
    }
}
