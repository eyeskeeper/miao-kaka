package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.vo.RankItemVO;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排行榜接口
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/rank")
@Tag(name = "排行榜")
@Slf4j
public class RankController {

    @Resource
    private UserService userService;

    @GetMapping("/streak")
    @Operation(summary = "全勤连击 Top 50", description = "按全勤连击天数排序（当天全部进行中计划都完成才累计）")
    public BaseResponse<List<RankItemVO>> streakRank() {
        return ResultUtils.success(userService.streakRank());
    }
}
