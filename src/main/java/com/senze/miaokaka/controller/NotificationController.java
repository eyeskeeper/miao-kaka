package com.senze.miaokaka.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.PageRequest;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.NotificationVO;
import com.senze.miaokaka.service.NotificationService;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户通知接口（拉取式；通用通知表，type 区分业务）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/notification")
@Tag(name = "用户通知")
@Slf4j
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserService userService;

    @GetMapping("/list")
    @Operation(summary = "我的通知分页", description = "id 倒序；pageSize 上限 50；读取不清空已读状态")
    public BaseResponse<Page<NotificationVO>> list(PageRequest pageRequest, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(notificationService.pageMy(
                pageRequest.getCurrent(), pageRequest.getPageSize(), user.getId()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数", description = "前端角标用")
    public BaseResponse<Long> unreadCount(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(notificationService.unreadCount(user.getId()));
    }

    @PutMapping("/read/{notificationId}")
    @Operation(summary = "标记单条已读", description = "仅能标记自己的通知；已读重复调用幂等成功")
    public BaseResponse<Boolean> markRead(@PathVariable Long notificationId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        notificationService.markRead(user.getId(), notificationId);
        return ResultUtils.success(true);
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读", description = "返回本次标记条数")
    public BaseResponse<Integer> markAllRead(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(notificationService.markAllRead(user.getId()));
    }
}
