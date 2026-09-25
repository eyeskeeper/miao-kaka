package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.HeatmapDayVO;
import com.senze.miaokaka.model.vo.WeeklyStatsVO;
import com.senze.miaokaka.service.CheckInRecordService;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 打卡统计接口（热力图 / 周报）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/check_in/stats")
@Tag(name = "打卡统计")
public class StatsController {

    @Resource
    private CheckInRecordService checkInRecordService;

    @Resource
    private UserService userService;

    @GetMapping("/heatmap")
    @Operation(summary = "年度打卡热力图", description = "当年每日打卡次数（正常+补卡），不传年份默认当年")
    public BaseResponse<List<HeatmapDayVO>> heatmap(@RequestParam(required = false) Integer year,
                                                    HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInRecordService.heatmap(user.getId(), year));
    }

    @GetMapping("/weekly")
    @Operation(summary = "本周统计周报", description = "周一~今天的每日计数、每计划完成天数、上周对比与 AI 总结")
    public BaseResponse<WeeklyStatsVO> weekly(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(checkInRecordService.weekly(user.getId()));
    }
}
