package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.duel.DuelCreateRequest;
import com.senze.miaokaka.model.dto.duel.ImpeachInitiateRequest;
import com.senze.miaokaka.model.dto.duel.ImpeachmentVoteRequest;
import com.senze.miaokaka.model.dto.duel.InviteUseRequest;
import com.senze.miaokaka.model.dto.duel.JoinApplicationReviewRequest;
import com.senze.miaokaka.model.dto.duel.ReviewRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.DuelVO;
import com.senze.miaokaka.model.vo.InviteInfoVO;
import com.senze.miaokaka.model.vo.InviteUseResultVO;
import com.senze.miaokaka.model.vo.InviteVO;
import com.senze.miaokaka.model.vo.JoinRequestVO;
import com.senze.miaokaka.model.vo.NudgeSentVO;
import com.senze.miaokaka.model.vo.ReviewItemVO;
import com.senze.miaokaka.service.DuelBattleService;
import com.senze.miaokaka.service.DuelImpeachmentService;
import com.senze.miaokaka.service.DuelInviteService;
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
    private DuelImpeachmentService duelImpeachmentService;

    @Resource
    private DuelInviteService duelInviteService;

    @Resource
    private UserService userService;

    @PostMapping
    @Operation(summary = "创建死斗", description = "创建者即组长并立即扣押金；默认明天开赛；招募中可加入/退出，开始后锁定")
    public BaseResponse<DuelVO> create(@Valid @RequestBody DuelCreateRequest request, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.create(user.getId(), request));
    }

    @GetMapping("/list")
    @Operation(summary = "我的死斗列表", description = "我创建的 + 我参与的 + 我有待审申请的")
    public BaseResponse<List<DuelVO>> list(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.listMine(user.getId()));
    }

    @GetMapping("/hall")
    @Operation(summary = "死斗大厅", description = "全量招募中+进行中的死斗分页（招募中优先、每页内最新在前，pageSize 上限 50）；轻量脱敏不带成员明细；myRelation=leader/member/applicant/null 标识我与该局的关系")
    public BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.senze.miaokaka.model.vo.DuelHallVO>> hall(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.hall(pageNum, pageSize, user.getId()));
    }

    @GetMapping("/{duelId}")
    @Operation(summary = "死斗详情", description = "成员/天数/奖池/我的状态；组长附待审核数")
    public BaseResponse<DuelVO> detail(@PathVariable Long duelId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.detail(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/join")
    @Operation(summary = "加入死斗", description = "仅招募中的自由加入死斗；扣押金；自动创建影子计划（含猫）")
    public BaseResponse<DuelVO> join(@PathVariable Long duelId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.join(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/apply")
    @Operation(summary = "申请加入", description = "仅审批加入模式；不扣押金，组长批准时才扣")
    public BaseResponse<DuelVO> apply(@PathVariable Long duelId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.apply(user.getId(), duelId));
    }

    @GetMapping("/{duelId}/applications")
    @Operation(summary = "待审加入申请列表", description = "仅组长（审批模式）")
    public BaseResponse<java.util.List<JoinRequestVO>> applications(@PathVariable Long duelId,
                                                                    HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.applications(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/applications/review")
    @Operation(summary = "审批加入申请", description = "仅组长；通过时扣押金入组，申请人喵币不足则自动拒绝并留痕")
    public BaseResponse<String> reviewApplication(@PathVariable Long duelId,
                                                  @Valid @RequestBody JoinApplicationReviewRequest request,
                                                  HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.reviewApplication(user.getId(), duelId, request));
    }

    @PostMapping("/{duelId}/members/{targetUserId}/remove")
    @Operation(summary = "移除成员", description = "仅组长；招募中=全额退款；进行中=即时结算（退剩余天数份额 押金×剩余天数/T，缺勤份额入罚没池）")
    public BaseResponse<DuelVO> removeMember(@PathVariable Long duelId,
                                             @PathVariable Long targetUserId,
                                             HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.removeMember(user.getId(), duelId, targetUserId));
    }

    @PostMapping("/{duelId}/transfer/{targetUserId}")
    @Operation(summary = "让渡组长", description = "仅组长；目标须为正式成员；即时生效，原组长降为普通成员")
    public BaseResponse<DuelVO> transfer(@PathVariable Long duelId,
                                         @PathVariable Long targetUserId,
                                         HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelService.transfer(user.getId(), duelId, targetUserId));
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

    @PostMapping("/{duelId}/impeach")
    @Operation(summary = "发起弹劾组长", description = "仅进行中死斗的正式成员（非组长）；原因≤20字；同一时刻仅一个进行中弹劾；失败后冷却24小时；发起人自动记1张弹劾票")
    public BaseResponse<DuelVO> impeach(@PathVariable Long duelId,
                                        @Valid @RequestBody ImpeachInitiateRequest request,
                                        HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        duelImpeachmentService.initiate(user.getId(), duelId, request);
        return ResultUtils.success(duelService.detail(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/impeachment/vote")
    @Operation(summary = "弹劾投票", description = "正式成员（含组长，组长只能投维持）对进行中弹劾投 维持(0)/弹劾(1)；一票定死不可改；弹劾票严格过半即成功，发起人立即成为新组长；24小时未过半自动判负")
    public BaseResponse<DuelVO> voteImpeachment(@PathVariable Long duelId,
                                                @Valid @RequestBody ImpeachmentVoteRequest request,
                                                HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        duelImpeachmentService.vote(user.getId(), duelId, request);
        return ResultUtils.success(duelService.detail(user.getId(), duelId));
    }

    @PostMapping("/{duelId}/invite")
    @Operation(summary = "生成邀请海报", description = "仅招募中的正式成员；每个（成员，死斗）固定一个邀请码，海报只渲染一次复用；二维码编码免登录落地 URL")
    public BaseResponse<InviteVO> invite(@PathVariable Long duelId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelInviteService.getOrCreateInvite(user.getId(), duelId));
    }

    @GetMapping("/invite/info/{code}")
    @Operation(summary = "邀请码落地信息", description = "免登录（扫码落地）；返回死斗摘要、邀请人与加入动作提示（direct=直接加入/apply=需审批）")
    public BaseResponse<InviteInfoVO> inviteInfo(@PathVariable String code) {
        return ResultUtils.success(duelInviteService.resolveInfo(code));
    }

    @PostMapping("/invite/use")
    @Operation(summary = "使用邀请码", description = "仅招募中；组长码两种模式都直接入组（审批制免审）；成员码：自由制直接加入、审批制提交申请并留痕邀请人")
    public BaseResponse<InviteUseResultVO> useInvite(@Valid @RequestBody InviteUseRequest request,
                                                     HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(duelInviteService.useInvite(user.getId(), request.getCode()));
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
