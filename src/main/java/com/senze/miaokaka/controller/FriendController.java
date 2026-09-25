package com.senze.miaokaka.controller;

import com.senze.miaokaka.common.BaseResponse;
import com.senze.miaokaka.common.ResultUtils;
import com.senze.miaokaka.model.dto.friend.FriendApplyRequest;
import com.senze.miaokaka.model.dto.friend.FriendVoteRequest;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.CheckInCalendarVO;
import com.senze.miaokaka.model.vo.FriendApplicationVO;
import com.senze.miaokaka.model.vo.FriendCatVO;
import com.senze.miaokaka.model.vo.FriendFeedItemVO;
import com.senze.miaokaka.model.vo.FriendRankItemVO;
import com.senze.miaokaka.model.vo.FriendSearchVO;
import com.senze.miaokaka.model.vo.FriendVO;
import com.senze.miaokaka.service.FriendService;
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

import java.util.List;
import java.util.Map;

/**
 * 好友接口（关系/申请/围观/点赞/动态/排行）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@RestController
@RequestMapping("/friend")
@Tag(name = "好友")
public class FriendController {

    @Resource
    private FriendService friendService;

    @Resource
    private UserService userService;

    @PostMapping("/apply")
    @Operation(summary = "发起好友申请", description = "申请-同意制；对方收到通知")
    public BaseResponse<Boolean> apply(@Valid @RequestBody FriendApplyRequest request,
                                       HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        friendService.apply(user.getId(), request.getTargetUserId());
        return ResultUtils.success(true);
    }

    @GetMapping("/applications")
    @Operation(summary = "收到的待审申请列表")
    public BaseResponse<List<FriendApplicationVO>> applications(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(friendService.applications(user.getId()));
    }

    @PostMapping("/agree")
    @Operation(summary = "同意申请", description = "双向写入好友，并通知申请人")
    public BaseResponse<Boolean> agree(@Valid @RequestBody FriendVoteRequest request,
                                       HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        friendService.agree(user.getId(), request.getId());
        return ResultUtils.success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "拒绝申请", description = "删除申请行，对方可重新申请")
    public BaseResponse<Boolean> reject(@Valid @RequestBody FriendVoteRequest request,
                                        HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        friendService.reject(user.getId(), request.getId());
        return ResultUtils.success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "我的好友列表", description = "含全勤连击")
    public BaseResponse<List<FriendVO>> list(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(friendService.listFriends(user.getId()));
    }

    @DeleteMapping("/{friendUserId}")
    @Operation(summary = "删除好友", description = "双向删除")
    public BaseResponse<Boolean> removeFriend(@PathVariable Long friendUserId,
                                              HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        friendService.removeFriend(user.getId(), friendUserId);
        return ResultUtils.success(true);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "按账号精确匹配或用户 id 定位，加好友前使用")
    public BaseResponse<FriendSearchVO> search(@RequestParam String keyword) {
        return ResultUtils.success(friendService.search(keyword));
    }

    @GetMapping("/rank")
    @Operation(summary = "好友排行", description = "好友 + 我，按全勤连击倒序")
    public BaseResponse<List<FriendRankItemVO>> rank(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(friendService.rank(user.getId()));
    }

    @GetMapping("/feed")
    @Operation(summary = "好友打卡动态", description = "好友最近 7 天打卡（正常+补卡），含点赞数与我是否已赞，最多 50 条")
    public BaseResponse<List<FriendFeedItemVO>> feed(HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(friendService.feed(user.getId()));
    }

    @GetMapping("/{friendId}/cats")
    @Operation(summary = "围观好友的猫", description = "仅好友；返回其进行中计划的猫列表")
    public BaseResponse<FriendCatVO> friendCats(@PathVariable Long friendId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(friendService.friendCats(user.getId(), friendId));
    }

    @GetMapping("/{friendId}/calendar")
    @Operation(summary = "围观好友打卡日历", description = "仅好友；只读查看好友某计划的月历")
    public BaseResponse<CheckInCalendarVO> friendCalendar(@PathVariable Long friendId,
                                                          @RequestParam Long planId,
                                                          @RequestParam(required = false) String month,
                                                          HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        return ResultUtils.success(friendService.friendCalendar(user.getId(), friendId, planId, month));
    }

    @PostMapping("/like/{recordId}")
    @Operation(summary = "点赞好友打卡", description = "同一记录仅可赞一次；每日限 5 次；被赞者小鱼干 +1")
    public BaseResponse<Map<String, Long>> like(@PathVariable Long recordId, HttpServletRequest servletRequest) {
        User user = userService.getLoginUser(servletRequest);
        long likeCount = friendService.like(user.getId(), recordId);
        return ResultUtils.success(Map.of("likeCount", likeCount));
    }
}
