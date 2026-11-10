package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.constant.NotificationConstant;
import com.senze.miaokaka.mapper.CheckInEvidenceMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.model.entity.CheckInEvidence;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.DuelAppealService;
import com.senze.miaokaka.service.DuelSettlementService;
import com.senze.miaokaka.service.NotificationService;
import com.senze.miaokaka.service.WalletService;
import com.senze.miaokaka.utils.SettlementCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 死斗结算实现：幂等、事务、宽容条款（结束时待审核自动通过）。
 * 每日缺卡分成：次日打卡即领昨日缺卡池均分（SLA 当日化保证昨日名单次日不可变）。
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

    private final DuelAppealService duelAppealService;

    private final NotificationService notificationService;

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

    @Override
    public void clawbackDaily(Duel duel, DuelMember member) {
        int daily = member.getDeposit() / duel.getTotalDays();
        int refunded = member.getRefunded() == null ? 0 : member.getRefunded();
        if (daily <= 0 || refunded <= 0) {
            return;
        }
        int clawback = Math.min(daily, refunded);
        walletService.clawbackDaily(member.getUserId(), clawback, duel.getId());
        member.setRefunded(refunded - clawback);
        duelMemberMapper.updateById(member);
    }

    @Override
    public int shareYesterdayPool(Duel duel, DuelMember member) {
        LocalDate yesterday = LocalDate.now(CheckInConstant.BIZ_ZONE).minusDays(1);
        // 第一天（及之前）没有昨日池
        if (yesterday.isBefore(duel.getStartDate())) {
            return 0;
        }
        List<DuelMember> actives = activeMembers(duel.getId());
        if (actives.isEmpty()) {
            return 0;
        }
        // 昨日有效打卡：有记录且凭证未被驳回（SLA 当日化后昨日凭证此刻只可能是 通过/驳回）
        Set<Long> validYesterdayUserIds = validCheckersOnDate(duel, actives, yesterday);
        if (!validYesterdayUserIds.contains(member.getUserId())) {
            // 昨日缺卡者今日回补：罚没已成定局，不追溯领取（Q1A）
            return 0;
        }
        // 昨日缺卡者：在册 − 有效打卡；池=各缺卡者当日份额之和
        int pool = actives.stream()
                .filter(m -> !validYesterdayUserIds.contains(m.getUserId()))
                .mapToInt(m -> m.getDeposit() / duel.getTotalDays())
                .sum();
        if (pool <= 0) {
            return 0;
        }
        int share = pool / validYesterdayUserIds.size();
        if (share <= 0) {
            return 0;
        }
        int absentees = actives.size() - validYesterdayUserIds.size();
        walletService.dailyShare(member.getUserId(), share, duel.getId(),
                "昨日 " + absentees + " 人缺卡，均分 " + share + " 喵币");
        // 已发总额记账：每次领取只累计本人实发额（原子自增，防结算超发）；
        // 同时逐出聚合缓存（agg 缓存了 duel 行）
        baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Duel>()
                .eq(Duel::getId, duel.getId())
                .setSql("daily_shared_out = daily_shared_out + " + share));
        cacheService.evict(CacheService.keyDuelAgg(duel.getId()));
        return share;
    }

    /**
     * 指定日期的"有效打卡"成员集合：有打卡记录且关联凭证未被驳回。
     * 待审（当日还没到 SLA 截止）按打卡计——宽容口径与超时自动通过一致。
     */
    private Set<Long> validCheckersOnDate(Duel duel, List<DuelMember> actives, LocalDate date) {
        List<Long> planIds = actives.stream().map(DuelMember::getPlanId).toList();
        List<CheckInRecord> records = checkInRecordMapper.selectList(
                new LambdaQueryWrapper<CheckInRecord>()
                        .in(CheckInRecord::getPlanId, planIds)
                        .eq(CheckInRecord::getCheckInDate, date));
        if (records.isEmpty()) {
            return Set.of();
        }
        // 被驳回的记录剔除（驳回=当日缺卡）
        Set<Long> rejectedRecordIds = checkInEvidenceMapper.selectList(
                        new LambdaQueryWrapper<CheckInEvidence>()
                                .in(CheckInEvidence::getRecordId,
                                        records.stream().map(CheckInRecord::getRecordId).toList())
                                .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_REJECTED))
                .stream().map(CheckInEvidence::getRecordId).collect(Collectors.toSet());
        java.util.Map<Long, Long> recordToUser = records.stream()
                .collect(Collectors.toMap(CheckInRecord::getRecordId, CheckInRecord::getUserId));
        return records.stream()
                .map(CheckInRecord::getRecordId)
                .filter(rid -> !rejectedRecordIds.contains(rid))
                .map(recordToUser::get)
                .collect(Collectors.toSet());
    }

    private List<DuelMember> activeMembers(Long duelId) {
        return duelMemberMapper.selectList(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .notIn(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT,
                        DuelConstant.MEMBER_STATUS_REMOVED));
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
        // 组内救济通道随局关闭：未决上诉统一失效（维持原判）
        duelAppealService.invalidatePending(duelId);

        // 结算范围：剩余在册成员（已退出/已移除的已即时结清，不再参与）
        List<DuelMember> members = duelMemberMapper.selectList(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .notIn(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT,
                        DuelConstant.MEMBER_STATUS_REMOVED));

        // 组队打卡：无押金无奖池——仅宽容通过待审凭证 + 成员落定，即完成结算
        if (duel.getMode() != null && duel.getMode() == DuelConstant.DUEL_MODE_TEAM) {
            for (DuelMember member : members) {
                member.setCheckinDays(countConfirmedDays(member, duel));
                duelMemberMapper.updateById(member);
            }
            memberSettled(members);
            log.info("组队打卡 {} 结算完成（无押金，无奖池），{} 名成员落定", duelId, members.size());
            return true;
        }

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

        // 最后一天缺卡池：无"次日打卡"发放通道，结算时均分给最后一天打卡者并入账+通知
        int lastDayShared = shareLastDayPool(duel, members);

        int totalPool = confiscatedSum + (duel.getRemovedPool() == null ? 0 : duel.getRemovedPool())
                - (duel.getDailySharedOut() == null ? 0 : duel.getDailySharedOut()) - lastDayShared;
        if (totalPool < 0) {
            // 防线：理论上改判垫付扣回已闭环账目；负值说明有异常路径，截断并告警供人工对账
            log.warn("死斗 {} 结算池为负（{}），按 0 截断，请人工对账", duelId, totalPool);
            totalPool = 0;
        }
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
     * 最后一天缺卡池（须在 autoApprovePending 之后：pending 已视为通过）：
     * 缺卡者=在册且 endDate 无记录 ∪ 凭证已驳回；池=各缺卡者当日份额之和，
     * 均分给最后一天有效打卡者，入账+站内通知（无次日打卡通道，通知即凭证）。
     * 返回实发总额（从总结算池扣减防超发）。
     */
    private int shareLastDayPool(Duel duel, List<DuelMember> members) {
        // 押金局才有池（组队局 deposit=0 池恒 0）
        if (members.stream().noneMatch(m -> m.getDeposit() / duel.getTotalDays() > 0)) {
            return 0;
        }
        Set<Long> validLastDay = validCheckersOnDate(duel, members, duel.getEndDate());
        if (validLastDay.isEmpty()) {
            return 0;
        }
        int absentees = (int) members.stream()
                .filter(m -> !validLastDay.contains(m.getUserId())).count();
        if (absentees == 0) {
            return 0;
        }
        int pool = members.stream()
                .filter(m -> !validLastDay.contains(m.getUserId()))
                .mapToInt(m -> m.getDeposit() / duel.getTotalDays())
                .sum();
        int share = pool / validLastDay.size();
        if (share <= 0) {
            return 0;
        }
        for (DuelMember m : members) {
            if (validLastDay.contains(m.getUserId())) {
                walletService.dailyShare(m.getUserId(), share, duel.getId(),
                        "最后一天 " + absentees + " 人缺卡，均分 " + share + " 喵币");
                notificationService.notify(m.getUserId(), NotificationConstant.TYPE_LASTDAY_SHARE,
                        "最后一天缺卡分成",
                        "「" + duel.getDuelName() + "」最后一天有 " + absentees
                                + " 人缺卡，你分得 " + share + " 喵币，已入账。",
                        duel.getId());
            }
        }
        int sharedTotal = share * validLastDay.size();
        log.info("死斗 {} 最后一天缺卡池：{} 喵币，实发 {}（{} 人均分）",
                duel.getId(), pool, sharedTotal, validLastDay.size());
        return sharedTotal;
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
