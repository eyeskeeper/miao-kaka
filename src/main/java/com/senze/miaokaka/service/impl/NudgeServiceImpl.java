package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.constant.NudgeConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.nudge.NudgeTemplateRequest;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.NudgeInboxVO;
import com.senze.miaokaka.model.vo.NudgeItemVO;
import com.senze.miaokaka.model.vo.NudgeSentVO;
import com.senze.miaokaka.model.vo.NudgeTemplateVO;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.NudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 拍一拍服务实现（无数据库表：消息与频控只存 Redis，当日过期）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NudgeServiceImpl implements NudgeService {

    private final UserMapper userMapper;

    private final DuelMapper duelMapper;

    private final DuelMemberMapper duelMemberMapper;

    private final CacheService cacheService;

    // region 模板

    @Override
    public NudgeTemplateVO saveTemplate(Long userId, NudgeTemplateRequest request) {
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        String text = StrUtil.trimToNull(request.getNudgeText());
        user.setNudgeText(text);
        userMapper.updateById(user);
        // 用户行已变，逐出登录态缓存
        cacheService.evict(CacheService.keyUser(userId));
        return toTemplateVO(text);
    }

    @Override
    public NudgeTemplateVO getTemplate(Long userId) {
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return toTemplateVO(user.getNudgeText());
    }

    private NudgeTemplateVO toTemplateVO(String stored) {
        NudgeTemplateVO vo = new NudgeTemplateVO();
        vo.setNudgeText(stored);
        vo.setEffectiveText(effectiveText(stored));
        return vo;
    }

    /**
     * 文案二级链：我的模板 → 系统默认
     */
    private static String effectiveText(String template) {
        return StrUtil.blankToDefault(template, NudgeConstant.DEFAULT_TEXT);
    }

    // endregion

    // region 拍一拍

    @Override
    public NudgeSentVO nudge(Long userId, Long duelId, Long targetUserId) {
        ThrowUtils.throwIf(targetUserId.equals(userId), ErrorCode.PARAMS_ERROR, "不能拍自己");
        ThrowUtils.throwIf(duelMapper.selectById(duelId) == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        requireActiveMember(duelId, userId, "你不是该死斗的正式成员");
        requireActiveMember(duelId, targetUserId, "对方不是该死斗的正式成员");
        User sender = userMapper.selectById(userId);
        User receiver = userMapper.selectById(targetUserId);
        ThrowUtils.throwIf(sender == null || receiver == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 1. 收件箱上限（先查箱再耗次数：投递失败不扣发送额度）
        String inboxKey = NudgeConstant.keyInbox(targetUserId);
        Long inboxSize = cacheService.listSize(inboxKey);
        if (inboxSize == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提醒功能暂不可用，请稍后再试");
        }
        ThrowUtils.throwIf(inboxSize >= NudgeConstant.INBOX_CAP,
                ErrorCode.OPERATION_ERROR, "对方的提醒箱已满，稍后再拍吧");

        // 2. 每日频控（北京时间 24 点重置）
        LocalDateTime now = LocalDateTime.now(CheckInConstant.BIZ_ZONE);
        String cntKey = NudgeConstant.keyDailyCount(userId, now.toLocalDate().toString());
        Long sent = cacheService.increment(cntKey, durationUntilMidnight(now));
        if (sent == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提醒功能暂不可用，请稍后再试");
        }
        ThrowUtils.throwIf(sent > NudgeConstant.DAILY_SEND_LIMIT,
                ErrorCode.OPERATION_ERROR, "今日拍一拍次数已用完（每天 " + NudgeConstant.DAILY_SEND_LIMIT + " 次）");

        // 3. 投递（当日过期）
        NudgeItemVO item = new NudgeItemVO();
        item.setFromUserId(userId);
        item.setFromUserName(sender.getUserName());
        item.setText(effectiveText(sender.getNudgeText()));
        item.setTime(now);
        cacheService.listPush(inboxKey, item, durationUntilMidnight(now));

        NudgeSentVO vo = new NudgeSentVO();
        vo.setToUserId(targetUserId);
        vo.setText(item.getText());
        vo.setDailyRemaining(Math.toIntExact(NudgeConstant.DAILY_SEND_LIMIT - sent));
        return vo;
    }

    @Override
    public NudgeInboxVO mine(Long userId) {
        String inboxKey = NudgeConstant.keyInbox(userId);
        java.util.List<NudgeItemVO> items = cacheService.listRange(inboxKey, NudgeItemVO.class);
        if (items == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提醒功能暂不可用，请稍后再试");
        }
        // 读取即消费
        cacheService.evict(inboxKey);
        NudgeInboxVO vo = new NudgeInboxVO();
        vo.setCount(items.size());
        vo.setItems(items);
        return vo;
    }

    // endregion

    private void requireActiveMember(Long duelId, Long userId, String message) {
        DuelMember member = duelMemberMapper.selectOne(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .eq(DuelMember::getUserId, userId));
        ThrowUtils.throwIf(member == null || member.getStatus() == DuelConstant.MEMBER_STATUS_QUIT,
                ErrorCode.FORBIDDEN_ERROR, message);
    }

    private static Duration durationUntilMidnight(LocalDateTime now) {
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(CheckInConstant.BIZ_ZONE).toLocalDateTime();
        Duration ttl = Duration.between(now, midnight);
        return ttl.isNegative() || ttl.isZero() ? Duration.ofMinutes(1) : ttl;
    }
}
