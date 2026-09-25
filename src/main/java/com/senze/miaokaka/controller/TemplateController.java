package com.senze.miaokaka.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.template.TemplateCreateRequest;
import com.senze.miaokaka.constant.UserConstant;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.TemplateVO;
import com.senze.miaokaka.service.TemplateService;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计划模板接口（模板市场）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/template")
@Tag(name = "计划模板")
public class TemplateController {

    @Resource
    private TemplateService templateService;

    @Resource
    private UserService userService;

    @GetMapping("/market")
    @Operation(summary = "模板市场", description = "官方模板置顶，其余按使用量/最新排序；每页 20 条")
    public BaseResponse<Page<TemplateVO>> market(@RequestParam(defaultValue = "1") long current,
                                                 HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(templateService.market(current, user.getId()));
    }

    @PostMapping
    @Operation(summary = "创建模板", description = "admin 创建自动为官方模板；普通用户创建进入公开市场；AI 草稿确认后也可存为模板")
    public BaseResponse<Long> create(@Valid @RequestBody TemplateCreateRequest request,
                                     HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(templateService.create(user.getId(),
                UserConstant.ADMIN_ROLE.equals(user.getUserRole()), request));
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "删除模板", description = "仅创建者或管理员；逻辑删除")
    public BaseResponse<Boolean> remove(@PathVariable Long templateId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        templateService.remove(user.getId(), UserConstant.ADMIN_ROLE.equals(user.getUserRole()), templateId);
        return ResultUtils.success(true);
    }
}
