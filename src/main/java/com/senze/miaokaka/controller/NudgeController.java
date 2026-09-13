package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.nudge.NudgeTemplateRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.NudgeInboxVO;
import com.senze.miaokaka.model.vo.NudgeTemplateVO;
import com.senze.miaokaka.service.NudgeService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 拍一拍接口（组内互相提醒打卡）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/nudge")
@Tag(name = "拍一拍")
@Slf4j
public class NudgeController {

    @Resource
    private NudgeService nudgeService;

    @Resource
    private UserService userService;

    @PutMapping("/template")
    @Operation(summary = "编辑我的拍一拍文案", description = "≤20 字；空串=清除模板，恢复系统默认「戳了戳你，快去打卡！」")
    public BaseResponse<NudgeTemplateVO> saveTemplate(@Valid @RequestBody NudgeTemplateRequest request,
                                                      HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(nudgeService.saveTemplate(user.getId(), request));
    }

    @GetMapping("/template")
    @Operation(summary = "查看我的拍一拍文案", description = "返回已存模板与实际生效文案")
    public BaseResponse<NudgeTemplateVO> getTemplate(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(nudgeService.getTemplate(user.getId()));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的待收拍一拍", description = "读取即消费：返回全部待收并清空收件箱")
    public BaseResponse<NudgeInboxVO> mine(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(nudgeService.mine(user.getId()));
    }
}
