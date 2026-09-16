package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.DuelImpeachmentMapper;
import com.senze.miaokaka.mapper.DuelImpeachmentVoteMapper;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.duel.ImpeachInitiateRequest;
import com.senze.miaokaka.model.dto.duel.ImpeachmentVoteRequest;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelImpeachment;
import com.senze.miaokaka.model.entity.DuelImpeachmentVote;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.DuelVO;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.DuelImpeachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;

/**
 * 死斗弹劾组长服务实现。
 * 规则：弹劾票严格大于正式成员数一半即成功（未投票视为支持组长）；
 * 24 小时内未过半自动判负；判负后该死斗冷却 24 小时；
 * 成功不冷却（可立即弹劾新组长）；全程不涉及资金。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelImpeachmentServiceImpl extends ServiceImpl<DuelImpeachmentMapper, DuelImpeachment>
        implements DuelImpeachmentService {

    private static final String FAIL_MAINTAIN_MAJORITY = "维持票过半";
    private static final String FAIL_EXPIRED = "投票截止，未过半";
    private static final String FAIL_LEADER_CHANGED = "组长已变更，弹劾终止";
    private static final String FAIL_INITIATOR_GONE = "发起人已离局";

    private final DuelImpeachmentVoteMapper duelImpeachmentVoteMapper;

    private final DuelMapper duelMapper;

    private final DuelMemberMapper duelMemberMapper;

    private final UserMapper userMapper;

    private final TransactionTemplate transactionTemplate;

    private final CacheService cacheService;

    @Override
    public void initiate(Long userId, Long duelId, ImpeachInitiateRequest request) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "仅进行中的死斗可以发起弹劾");
        ThrowUtils.throwIf(duel.getLeaderId().equals(userId),
                ErrorCode.PARAMS_ERROR, "组长不能发起弹劾，弹劾的对象就是组长");
        requireActiveMember(duelId, userId, "仅正式成员可以发起弹劾");
        ThrowUtils.throwIf(countActive(duelId) > 0,
                ErrorCode.OPERATION_ERROR, "已有进行中的弹劾，请等待投票结果");
        // 冷却：最近一次判负弹劾的结束时间 + 24 小时
        DuelImpeachment lastFailed = list(new LambdaQueryWrapper<DuelImpeachment>()
                .eq(DuelImpeachment::getDuelId, duelId)
                .eq(DuelImpeachment::getStatus, DuelConstant.IMPEACH_STATUS_FAILED)
                .orderByDesc(DuelImpeachment::getFinishTime))
                .stream().findFirst().orElse(null);
        if (lastFailed != null && lastFailed.getFinishTime() != null) {
            long sinceFail = System.currentTimeMillis() - lastFailed.getFinishTime().getTime();
            ThrowUtils.throwIf(sinceFail < DuelConstant.IMPEACH_COOLDOWN_HOURS * 3600_000L,
                    ErrorCode.OPERATION_ERROR, "弹劾失败后需冷却 24 小时，稍后再试");
        }
        transactionTemplate.execute(status -> {
            DuelImpeachment imp = new DuelImpeachment();
            imp.setDuelId(duelId);
            imp.setInitiatorId(userId);
            imp.setReason(request.getReason().trim());
            imp.setStatus(DuelConstant.IMPEACH_STATUS_ACTIVE);
            // 发起人默认弹劾票
            imp.setImpeachCnt(1);
            imp.setMaintainCnt(0);
            imp.setExpireTime(new Date(System.currentTimeMillis()
                    + DuelConstant.IMPEACH_DURATION_HOURS * 3600_000L));
            save(imp);
            DuelImpeachmentVote vote = new DuelImpeachmentVote();
            vote.setImpeachmentId(imp.getId());
            vote.setDuelId(duelId);
            vote.setUserId(userId);
            vote.setVote(DuelConstant.VOTE_IMPEACH);
            duelImpeachmentVoteMapper.insert(vote);
            return null;
        });
        cacheService.evict(CacheService.keyDuelAgg(duelId));
    }

    @Override
    public void vote(Long userId, Long duelId, ImpeachmentVoteRequest request) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "死斗已结束，弹劾投票关闭");
        DuelImpeachment imp = getById(request.getImpeachmentId());
        ThrowUtils.throwIf(imp == null || !imp.getDuelId().equals(duelId),
                ErrorCode.NOT_FOUND_ERROR, "弹劾不存在");
        ThrowUtils.throwIf(imp.getStatus() != DuelConstant.IMPEACH_STATUS_ACTIVE,
                ErrorCode.OPERATION_ERROR, "该弹劾已结束");
        ThrowUtils.throwIf(imp.getExpireTime() != null && imp.getExpireTime().before(new Date()),
                ErrorCode.OPERATION_ERROR, "弹劾投票已截止");
        int choice = request.getVote();
        boolean isImpeach = choice == DuelConstant.VOTE_IMPEACH;
        if (duel.getLeaderId().equals(userId)) {
            ThrowUtils.throwIf(isImpeach, ErrorCode.OPERATION_ERROR, "组长不能给自己投弹劾票");
        }
        requireActiveMember(duelId, userId, "仅正式成员可以投票");
        transactionTemplate.execute(status -> {
            try {
                DuelImpeachmentVote vote = new DuelImpeachmentVote();
                vote.setImpeachmentId(imp.getId());
                vote.setDuelId(duelId);
                vote.setUserId(userId);
                vote.setVote(choice);
                duelImpeachmentVoteMapper.insert(vote);
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "你已投过票，一票定死不可改");
            }
            // 原子计数列累加，免 count 查询
            update(new LambdaUpdateWrapper<DuelImpeachment>()
                    .eq(DuelImpeachment::getId, imp.getId())
                    .setSql(isImpeach ? "impeach_cnt = impeach_cnt + 1" : "maintain_cnt = maintain_cnt + 1"));
            DuelImpeachment fresh = getById(imp.getId());
            int total = duel.getMemberCount() == null ? 0 : duel.getMemberCount();
            if (total <= 0) {
                return null;
            }
            if (fresh.getImpeachCnt() * 2 > total) {
                trySuccess(duel, fresh);
            } else if (fresh.getMaintainCnt() * 2 > total) {
                // 维持票已过半，弹劾在数学上不可能再过半 → 提前判负
                resolveFailed(fresh, FAIL_MAINTAIN_MAJORITY);
            }
            return null;
        });
        cacheService.evict(CacheService.keyDuelAgg(duelId));
    }

    @Override
    public int resolveExpired() {
        List<DuelImpeachment> expired = list(new LambdaQueryWrapper<DuelImpeachment>()
                .eq(DuelImpeachment::getStatus, DuelConstant.IMPEACH_STATUS_ACTIVE)
                .le(DuelImpeachment::getExpireTime, new Date()));
        int count = 0;
        for (DuelImpeachment imp : expired) {
            if (resolveFailed(imp, FAIL_EXPIRED)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public DuelVO.ImpeachmentVO assembleActiveVO(Long duelId, Integer totalCount) {
        DuelImpeachment active = currentActive(duelId);
        if (active == null) {
            return null;
        }
        if (active.getExpireTime() != null && active.getExpireTime().before(new Date())) {
            // 详情访问惰性兜底：过期未结算的当场判负
            resolveFailed(active, FAIL_EXPIRED);
            return null;
        }
        DuelVO.ImpeachmentVO vo = new DuelVO.ImpeachmentVO();
        vo.setId(active.getId());
        vo.setInitiatorId(active.getInitiatorId());
        vo.setReason(active.getReason());
        vo.setImpeachCount(active.getImpeachCnt());
        vo.setMaintainCount(active.getMaintainCnt());
        vo.setTotalCount(totalCount);
        vo.setExpireTime(active.getExpireTime());
        User initiator = userMapper.selectById(active.getInitiatorId());
        if (initiator != null) {
            vo.setInitiatorName(initiator.getUserName());
        }
        return vo;
    }

    @Override
    public Long getActiveInitiator(Long duelId) {
        DuelImpeachment active = currentActive(duelId);
        if (active == null
                || (active.getExpireTime() != null && active.getExpireTime().before(new Date()))) {
            return null;
        }
        return active.getInitiatorId();
    }

    @Override
    public void terminateOnLeaderChange(Long duelId) {
        DuelImpeachment active = currentActive(duelId);
        if (active != null) {
            resolveFailed(active, FAIL_LEADER_CHANGED);
        }
    }

    @Override
    public Integer myVoteOf(Long impeachmentId, Long userId) {
        DuelImpeachmentVote vote = duelImpeachmentVoteMapper.selectOne(
                new LambdaQueryWrapper<DuelImpeachmentVote>()
                        .eq(DuelImpeachmentVote::getImpeachmentId, impeachmentId)
                        .eq(DuelImpeachmentVote::getUserId, userId));
        return vote == null ? null : vote.getVote();
    }

    /**
     * 弹劾成功：发起人接任组长（幂等闸门防并发双换）；发起人极端并发下已离局则判负
     */
    private void trySuccess(Duel duel, DuelImpeachment fresh) {
        DuelMember initiator = duelMemberMapper.selectOne(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duel.getId())
                .eq(DuelMember::getUserId, fresh.getInitiatorId()));
        if (initiator == null || initiator.getStatus() == DuelConstant.MEMBER_STATUS_QUIT
                || initiator.getStatus() == DuelConstant.MEMBER_STATUS_REMOVED) {
            resolveFailed(fresh, FAIL_INITIATOR_GONE);
            return;
        }
        boolean claimed = update(new LambdaUpdateWrapper<DuelImpeachment>()
                .eq(DuelImpeachment::getId, fresh.getId())
                .eq(DuelImpeachment::getStatus, DuelConstant.IMPEACH_STATUS_ACTIVE)
                .set(DuelImpeachment::getStatus, DuelConstant.IMPEACH_STATUS_SUCCESS)
                .set(DuelImpeachment::getFinishTime, new Date()));
        if (claimed) {
            // 角色由 duel.leader_id 派生：换人即生效，老组长自动降为普通成员
            Long oldLeaderId = duel.getLeaderId();
            duel.setLeaderId(fresh.getInitiatorId());
            duelMapper.updateById(duel);
            log.info("死斗 {} 弹劾成功，组长 {} → {}", duel.getId(), oldLeaderId, fresh.getInitiatorId());
        }
    }

    private boolean resolveFailed(DuelImpeachment imp, String failReason) {
        boolean claimed = update(new LambdaUpdateWrapper<DuelImpeachment>()
                .eq(DuelImpeachment::getId, imp.getId())
                .eq(DuelImpeachment::getStatus, DuelConstant.IMPEACH_STATUS_ACTIVE)
                .set(DuelImpeachment::getStatus, DuelConstant.IMPEACH_STATUS_FAILED)
                .set(DuelImpeachment::getFailReason, failReason)
                .set(DuelImpeachment::getFinishTime, new Date()));
        if (claimed) {
            cacheService.evict(CacheService.keyDuelAgg(imp.getDuelId()));
        }
        return claimed;
    }

    private DuelImpeachment currentActive(Long duelId) {
        List<DuelImpeachment> actives = list(new LambdaQueryWrapper<DuelImpeachment>()
                .eq(DuelImpeachment::getDuelId, duelId)
                .eq(DuelImpeachment::getStatus, DuelConstant.IMPEACH_STATUS_ACTIVE)
                .orderByDesc(DuelImpeachment::getId));
        return actives.isEmpty() ? null : actives.get(0);
    }

    private long countActive(Long duelId) {
        return count(new LambdaQueryWrapper<DuelImpeachment>()
                .eq(DuelImpeachment::getDuelId, duelId)
                .eq(DuelImpeachment::getStatus, DuelConstant.IMPEACH_STATUS_ACTIVE));
    }

    private void requireActiveMember(Long duelId, Long userId, String message) {
        DuelMember member = duelMemberMapper.selectOne(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .eq(DuelMember::getUserId, userId));
        ThrowUtils.throwIf(member == null
                        || member.getStatus() == DuelConstant.MEMBER_STATUS_QUIT
                        || member.getStatus() == DuelConstant.MEMBER_STATUS_REMOVED,
                ErrorCode.OPERATION_ERROR, message);
    }
}
