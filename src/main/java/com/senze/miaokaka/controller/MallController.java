package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.mall.FishExchangeRequest;
import com.senze.miaokaka.model.dto.mall.MallBuyRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.BagItemVO;
import com.senze.miaokaka.model.vo.MallBuyResultVO;
import com.senze.miaokaka.model.vo.MallItemVO;
import com.senze.miaokaka.service.MallService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 积分商城接口
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/mall")
@Tag(name = "积分商城")
@Slf4j
public class MallController {

    @Resource
    private MallService mallService;

    @Resource
    private UserService userService;

    @GetMapping("/catalog")
    @Operation(summary = "商品目录", description = "全部商品 + 我的持有数量")
    public BaseResponse<List<MallItemVO>> catalog(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(mallService.catalog(user.getId()));
    }

    @GetMapping("/bag")
    @Operation(summary = "我的背包")
    public BaseResponse<List<BagItemVO>> bag(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(mallService.bag(user.getId()));
    }

    @PostMapping("/buy")
    @Operation(summary = "购买道具", description = "原子扣积分（不足 40000 拒绝），背包数量 +1")
    public BaseResponse<MallBuyResultVO> buy(@Valid @RequestBody MallBuyRequest request,
                                             HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(mallService.buy(user.getId(), request.getItemCode()));
    }

    @PostMapping("/exchange-fish")
    @Operation(summary = "小鱼干兑积分", description = "好友点赞获得的小鱼干 1:1 兑换积分；原子扣减，余额不足拒绝")
    public BaseResponse<Integer> exchangeFish(@Valid @RequestBody FishExchangeRequest request,
                                              HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(mallService.exchangeFish(user.getId(), request.getFish()));
    }
}
