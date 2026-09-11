package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.service.StorageService;
import com.senze.miaokaka.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 通用图片上传接口
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/upload")
@Tag(name = "文件上传")
@Slf4j
public class UploadController {

    @Resource
    private StorageService storageService;

    @Resource
    private UserService userService;

    @PostMapping("/image")
    @Operation(summary = "上传图片", description = "≤5MB，jpg/jpeg/png/webp；返回可访问的 /uploads/... URL")
    public BaseResponse<Map<String, String>> image(@RequestParam("file") MultipartFile file,
                                                   HttpServletRequest servletRequest) {
        userService.getLoginUser(servletRequest);
        String url = storageService.storeImage(file);
        return ResultUtils.success(Map.of("url", url));
    }
}
