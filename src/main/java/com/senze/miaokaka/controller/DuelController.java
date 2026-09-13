package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.duel.DuelCreateRequest;
import com.senze.miaokaka.model.dto.duel.ReviewRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.DuelVO;
import com.senze.miaokaka.model.vo.NudgeSentVO;
import com.senze.miaokaka.model.vo.ReviewItemVO;
import com.senze.miaokaka.service.DuelBattleService;
import com.senze.miaokaka.service.DuelService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 习惯死斗接口
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/duel")
@Tag(name = "习惯死斗")
@Slf4j
public class DuelController {

    @Resource
    private DuelService duelService;

    @Resource
    private DuelBattleService duelBattleService;

    @Resource
    private NudgeService nudgeService;

    @Resource
    private UserService userService;

    @PostMapping
    @Operation(summary = "创建死斗", description = "创建者即组长并立即扣押金；默认明天开赛；招募中可加入/退出，开始后锁定")
    public BaseResponse<DuelVO> create(@Valid @RequestBody DuelCreateRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.create(user.getId(), request));
    }

    @GetMapping("/list")
    @Operation(summary = "我的死斗列表", description = "我创建的 + 我参与的")
    public BaseResponse<List<DuelVO>> list(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.listMine(user.getId()));
    }

    @GetMapping("/{duelId}")
    @Operation(summary = "死斗详情", description = "成员/天数/奖池/我的状态；组长附待审核数")
    public BaseResponse<DuelVO> detail(@PathVariable Long duelId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.detail(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/join")
    @Operation(summary = "加入死斗", description = "仅招募中；扣押金；自动创建影子计划（含猫）")
    public BaseResponse<DuelVO> join(@PathVariable Long duelId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.join(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/quit")
    @Operation(summary = "退出死斗", description = "仅招募中；全额退款")
    public BaseResponse<DuelVO> quit(@PathVariable Long duelId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.quit(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/nudge/{targetUserId}")
    @Operation(summary = "拍一下", description = "同死斗成员互拍；文案=我的模板→系统默认；发起方每日限 5 次（北京午夜重置）；对方收件箱上限 20 条（当日过期）；消息不落数据库")
    public BaseResponse<NudgeSentVO> nudge(@PathVariable Long duelId,
                                           @PathVariable Long targetUserId,
                                           HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(nudgeService.nudge(user.getId(), duelId, targetUserId));
    }

    @PostMapping("/{duelId}/check_in")
    @Operation(summary = "死斗打卡", description = "multipart 上传照片凭证（≤5MB jpg/png/webp）；记录进入待审核，组长（或AI）审核通过才生效")
    public BaseResponse<CheckInResultVO> checkIn(@PathVariable Long duelId,
                                                 @RequestParam("image") MultipartFile image,
                                                 @RequestParam(value = "remark", required = false) String remark,
                                                 HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelBattleService.checkIn(user.getId(), duelId, image, remark));
    }

    @GetMapping("/{duelId}/review/pending")
    @Operation(summary = "待审核凭证列表", description = "仅组长")
    public BaseResponse<List<ReviewItemVO>> pendingList(@PathVariable Long duelId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelBattleService.pendingList(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/review")
    @Operation(summary = "审核凭证", description = "仅组长；通过触发猫事件结算，驳回必须给理由；结束时的待审凭证结算时自动通过")
    public BaseResponse<CheckInResultVO> review(@PathVariable Long duelId,
                                                @Valid @RequestBody ReviewRequest request,
                                                HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelBattleService.review(user.getId(), duelId, request));
    }
}
