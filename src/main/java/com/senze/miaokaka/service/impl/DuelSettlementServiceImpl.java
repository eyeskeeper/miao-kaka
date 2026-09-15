package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.mapper.CheckInEvidenceMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.model.entity.CheckInEvidence;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.DuelSettlementService;
import com.senze.miaokaka.service.WalletService;
import com.senze.miaokaka.utils.SettlementCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 死斗结算实现：幂等、事务、宽容条款（结束时待审核自动通过）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelSettlementServiceImpl extends ServiceImpl<DuelMapper, Duel>
        implements DuelSettlementService {

    private final DuelMemberMapper duelMemberMapper;

    private final CheckInRecordMapper checkInRecordMapper;

    private final CheckInEvidenceMapper checkInEvidenceMapper;

    private final WalletService walletService;

    private final TransactionTemplate transactionTemplate;

    private final CacheService cacheService;

    @Override
    public void settleAllDue() {
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        List<Duel> due = list(new LambdaQueryWrapper<Duel>()
                .eq(Duel::getStatus, DuelConstant.DUEL_STATUS_RUNNING)
                .eq(Duel::getSettled, 0)
                .lt(Duel::getEndDate, today));
        for (Duel duel : due) {
            try {
                settleIfDue(duel.getId());
            } catch (Exception e) {
                log.error("死斗 {} 结算失败", duel.getId(), e);
            }
        }
    }

    @Override
    public boolean settleIfDue(Long duelId) {
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        Duel duel = getById(duelId);
        if (duel == null || duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING
                || duel.getSettled() != null && duel.getSettled() == 1
                || !duel.getEndDate().isBefore(today)) {
            return false;
        }
        transactionTemplate.execute(status -> doSettle(duelId, today));
        return true;
    }

    @Override
    public void dailyRefund(Duel duel, DuelMember member) {
        int daily = member.getDeposit() / duel.getTotalDays();
        if (daily <= 0) {
            return;
        }
        walletService.dailyRefund(member.getUserId(), daily, duel.getId());
        member.setRefunded((member.getRefunded() == null ? 0 : member.getRefunded()) + daily);
        duelMemberMapper.updateById(member);
    }

    private Boolean doSettle(Long duelId, LocalDate today) {
        // 幂等闸门：条件更新抢占结算权，并发下只有一个事务能成功
        boolean claimed = update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Duel>()
                .eq(Duel::getId, duelId)
                .eq(Duel::getSettled, 0)
                .set(Duel::getSettled, 1)
                .set(Duel::getStatus, DuelConstant.DUEL_STATUS_SETTLED));
        if (!claimed) {
            return false;
        }
        // 结算改变成员状态/天数展示，逐出聚合缓存（钱包余额本就不缓存）
        cacheService.evict(CacheService.keyDuelAgg(duelId));
        Duel duel = getById(duelId);

        // 宽容条款：结束时任然待审核的记录自动视为通过
        autoApprovePending(duelId);

        // 结算范围：剩余在册成员（已退出/已移除的已即时结清，不再参与）
        List<DuelMember> members = duelMemberMapper.selectList(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .notIn(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT,
                        DuelConstant.MEMBER_STATUS_REMOVED));

        // 新口径：end_refund = floor(押金×D/T) − 已退（补齐舍入尾差）；
        // 罚没 = 押金 − 已退 − end_refund；奖池 = Σ罚没 + 被移除成员罚没池
        int confiscatedSum = 0;
        List<SettlementCalculator.MemberStake> stakes = new java.util.ArrayList<>(members.size());
        for (DuelMember member : members) {
            int days = countConfirmedDays(member, duel);
            int refunded = member.getRefunded() == null ? 0 : member.getRefunded();
            member.setCheckinDays(days);
            duelMemberMapper.updateById(member);
            int endRefund = Math.max(0,
                    member.getDeposit() * days / duel.getTotalDays() - refunded);
            int confiscated = Math.max(0,
                    member.getDeposit() - refunded - endRefund);
            confiscatedSum += confiscated;
            walletService.refund(member.getUserId(), endRefund, duelId,
                    "死斗结算：按 " + days + "/" + duel.getTotalDays() + " 天补齐退还");
            stakes.add(new SettlementCalculator.MemberStake(member.getUserId(), member.getDeposit(), days));
        }

        int totalPool = confiscatedSum + (duel.getRemovedPool() == null ? 0 : duel.getRemovedPool());
        SettlementCalculator.SplitResult split =
                SettlementCalculator.splitByDays(totalPool, stakes);
        for (SettlementCalculator.MemberStake stake : stakes) {
            int share = split.shares().getOrDefault(stake.userId(), 0);
            walletService.awardPoolShare(stake.userId(), share, duelId);
        }
        if (split.remainder() > 0) {
            log.info("死斗 {} 结算完成，沉没 {} 喵币（无人可分）", duelId, split.remainder());
        }
        memberSettled(members);
        return true;
    }

    /**
     * 结束时任然待审核 → 自动通过（组长不作为不坑成员的钱），不计事件结算
     */
    private void autoApprovePending(Long duelId) {
        List<CheckInEvidence> pendings = checkInEvidenceMapper.selectList(
                new LambdaQueryWrapper<CheckInEvidence>()
                        .eq(CheckInEvidence::getDuelId, duelId)
                        .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING));
        for (CheckInEvidence evidence : pendings) {
            evidence.setReviewStatus(DuelConstant.REVIEW_STATUS_APPROVED);
            evidence.setReviewRemark("挑战结束，待审核凭证自动视为通过");
            evidence.setReviewTime(new Date());
            checkInEvidenceMapper.updateById(evidence);
            CheckInRecord record = checkInRecordMapper.selectById(evidence.getRecordId());
            if (record != null && record.getStatus() == CheckInConstant.RECORD_STATUS_PENDING) {
                record.setStatus(CheckInConstant.RECORD_STATUS_NORMAL);
                checkInRecordMapper.updateById(record);
            }
        }
    }

    private int countConfirmedDays(DuelMember member, Duel duel) {
        return Math.toIntExact(checkInRecordMapper.selectCount(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getPlanId, member.getPlanId())
                .eq(CheckInRecord::getStatus, CheckInConstant.RECORD_STATUS_NORMAL)
                .ge(CheckInRecord::getCheckInDate, duel.getStartDate())
                .le(CheckInRecord::getCheckInDate, duel.getEndDate())));
    }

    private void memberSettled(List<DuelMember> members) {
        for (DuelMember member : members) {
            member.setStatus(DuelConstant.MEMBER_STATUS_SETTLED);
            duelMemberMapper.updateById(member);
        }
    }
}
