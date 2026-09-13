package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.RankBoardVO;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "全勤连击榜", description = "Top 50 + 当前用户排名 myRank（Top 50 内取榜单名次，50 外实时补算，零连击为 null）")
    public BaseResponse<RankBoardVO> streakRank(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(userService.streakBoard(user.getId()));
    }
}
