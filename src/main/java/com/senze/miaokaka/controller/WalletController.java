package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.WalletVO;
import com.senze.miaokaka.service.UserService;
import com.senze.miaokaka.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 喵币钱包接口
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/wallet")
@Tag(name = "喵币钱包")
@Slf4j
public class WalletController {

    @Resource
    private WalletService walletService;

    @Resource
    private UserService userService;

    @GetMapping
    @Operation(summary = "余额与流水", description = "注册赠 1000；押金支出/退还/奖池分得均有流水可查")
    public BaseResponse<WalletVO> wallet(@RequestParam(value = "current", defaultValue = "1") int current,
                                         @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                         HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(walletService.walletDetail(user.getId(), current, pageSize));
    }
}
