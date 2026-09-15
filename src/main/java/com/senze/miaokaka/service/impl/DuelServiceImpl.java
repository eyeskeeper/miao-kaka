package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelJoinRequestMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.mapper.CheckInEvidenceMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.duel.DuelCreateRequest;
import com.senze.miaokaka.model.dto.duel.JoinApplicationReviewRequest;
import com.senze.miaokaka.model.entity.CheckInEvidence;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelJoinRequest;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.DuelAggData;
import com.senze.miaokaka.model.vo.DuelVO;
import com.senze.miaokaka.model.vo.JoinRequestVO;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.CheckInPlanService;
import com.senze.miaokaka.service.DuelService;
import com.senze.miaokaka.service.DuelSettlementService;
import com.senze.miaokaka.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 习惯死斗生命周期服务实现
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelServiceImpl extends ServiceImpl<DuelMapper, Duel> implements DuelService {

    private final DuelMemberMapper duelMemberMapper;

    private final DuelJoinRequestMapper duelJoinRequestMapper;

    private final CheckInPlanService checkInPlanService;

    private final CheckInRecordMapper checkInRecordMapper;

    private final CheckInEvidenceMapper checkInEvidenceMapper;

    private final UserMapper userMapper;

    private final WalletService walletService;

    private final DuelSettlementService duelSettlementService;

    private final TransactionTemplate transactionTemplate;

    private final CacheService cacheService;

    // region 生命周期

    @Override
    public DuelVO create(Long userId, DuelCreateRequest request) {
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        LocalDate startDate;
        if (StrUtil.isBlank(request.getStartDate())) {
            startDate = today.plusDays(1);
        } else {
            try {
                startDate = LocalDate.parse(request.getStartDate());
            } catch (DateTimeParseException e) {
                throw new com.senze.miaokaka.exception.BusinessException(
                        ErrorCode.PARAMS_ERROR, "开始日期格式应为 yyyy-MM-dd");
            }
        }
        ThrowUtils.throwIf(startDate.isBefore(today), ErrorCode.PARAMS_ERROR, "开始日期不能早于今天");

        Duel duel = transactionTemplate.execute(status -> {
            Duel d = new Duel();
            d.setDuelName(request.getDuelName().trim());
            d.setDuelDesc(StrUtil.blankToDefault(request.getDuelDesc(), null));
            d.setLeaderId(userId);
            d.setJoinMode(request.getJoinMode() == null ? DuelConstant.JOIN_MODE_FREE : request.getJoinMode());
            d.setDepositPerMember(request.getDepositPerMember());
            d.setTotalDays(request.getTotalDays());
            d.setStartDate(startDate);
            d.setEndDate(startDate.plusDays(request.getTotalDays() - 1L));
            d.setStatus(DuelConstant.DUEL_STATUS_RECRUITING);
            d.setMemberCount(0);
            d.setTotalPool(0);
            d.setSettled(0);
            save(d);
            // 创建者即第一名成员（加入流程含扣押金 + 影子计划）
            joinDuelTx(d, userId);
            return d;
        });
        return detail(userId, duel.getId());
    }

    @Override
    public DuelVO join(Long userId, Long duelId) {
        Duel duel = getRequiredDuel(duelId);
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING,
                ErrorCode.OPERATION_ERROR, "该死斗已开始或结束，无法加入");
        ThrowUtils.throwIf(duel.getJoinMode() != null && duel.getJoinMode() == DuelConstant.JOIN_MODE_APPROVAL,
                ErrorCode.OPERATION_ERROR, "该死斗为审批加入制，请先提交加入申请");
        long joined = duelMemberMapper.selectCount(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .eq(DuelMember::getUserId, userId)
                .ne(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT));
        ThrowUtils.throwIf(joined > 0, ErrorCode.OPERATION_ERROR, "你已在该死斗中");
        transactionTemplate.execute(status -> {
            joinDuelTx(duel, userId);
            return null;
        });
        return detail(userId, duelId);
    }

    @Override
    public DuelVO apply(Long userId, Long duelId) {
        Duel duel = getRequiredDuel(duelId);
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING,
                ErrorCode.OPERATION_ERROR, "该死斗已开始或结束，无法申请");
        ThrowUtils.throwIf(duel.getJoinMode() == null || duel.getJoinMode() != DuelConstant.JOIN_MODE_APPROVAL,
                ErrorCode.OPERATION_ERROR, "该死斗为自由加入，直接加入即可");
        long joined = duelMemberMapper.selectCount(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .eq(DuelMember::getUserId, userId)
                .ne(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT));
        ThrowUtils.throwIf(joined > 0, ErrorCode.OPERATION_ERROR, "你已在该死斗中");
        long pending = duelJoinRequestMapper.selectCount(new LambdaQueryWrapper<DuelJoinRequest>()
                .eq(DuelJoinRequest::getDuelId, duelId)
                .eq(DuelJoinRequest::getUserId, userId)
                .eq(DuelJoinRequest::getStatus, DuelConstant.JOIN_REQUEST_PENDING));
        ThrowUtils.throwIf(pending > 0, ErrorCode.OPERATION_ERROR, "你已有待审批的加入申请");
        DuelJoinRequest request = new DuelJoinRequest();
        request.setDuelId(duelId);
        request.setUserId(userId);
        request.setStatus(DuelConstant.JOIN_REQUEST_PENDING);
        duelJoinRequestMapper.insert(request);
        return detail(userId, duelId);
    }

    @Override
    public List<JoinRequestVO> applications(Long userId, Long duelId) {
        Duel duel = getRequiredDuel(duelId);
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可查看加入申请");
        List<DuelJoinRequest> pendings = duelJoinRequestMapper.selectList(
                new LambdaQueryWrapper<DuelJoinRequest>()
                        .eq(DuelJoinRequest::getDuelId, duelId)
                        .eq(DuelJoinRequest::getStatus, DuelConstant.JOIN_REQUEST_PENDING)
                        .orderByAsc(DuelJoinRequest::getId));
        Map<Long, User> users = pendings.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(pendings.stream().map(DuelJoinRequest::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return pendings.stream().map(r -> {
            JoinRequestVO vo = new JoinRequestVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            User user = users.get(r.getUserId());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).toList();
    }

    @Override
    public String reviewApplication(Long userId, Long duelId, JoinApplicationReviewRequest request) {
        Duel duel = getRequiredDuel(duelId);
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可审批加入申请");
        boolean approve = Boolean.TRUE.equals(request.getApprove());
        DuelJoinRequest joinRequest = duelJoinRequestMapper.selectById(request.getRequestId());
        ThrowUtils.throwIf(joinRequest == null || !joinRequest.getDuelId().equals(duelId),
                ErrorCode.NOT_FOUND_ERROR, "加入申请不存在");
        ThrowUtils.throwIf(joinRequest.getStatus() != DuelConstant.JOIN_REQUEST_PENDING,
                ErrorCode.OPERATION_ERROR, "该申请已处理过");
        if (!approve) {
            finishRequest(joinRequest, DuelConstant.JOIN_REQUEST_REJECTED,
                    StrUtil.blankToDefault(request.getRemark(), "组长拒绝"));
            return "已拒绝该加入申请";
        }
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING,
                ErrorCode.OPERATION_ERROR, "死斗已开始，无法再通过申请");
        ThrowUtils.throwIf(duel.getMemberCount() >= DuelConstant.MEMBER_MAX,
                ErrorCode.OPERATION_ERROR, "该死斗已满员，无法通过申请");
        long joined = duelMemberMapper.selectCount(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .eq(DuelMember::getUserId, joinRequest.getUserId())
                .ne(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT));
        ThrowUtils.throwIf(joined > 0, ErrorCode.OPERATION_ERROR, "申请人已在该死斗中");

        // 余额预检：不足则自动拒绝并留痕（不消耗申请机会以外的任何东西）
        int balance = walletService.getBalance(joinRequest.getUserId());
        if (balance < duel.getDepositPerMember()) {
            finishRequest(joinRequest, DuelConstant.JOIN_REQUEST_REJECTED,
                    "喵币不足（需 " + duel.getDepositPerMember() + "），自动拒绝");
            return "申请人喵币不足，已自动拒绝";
        }
        transactionTemplate.execute(status -> {
            // 扣押金 + 建影子计划 + 入组（预检与扣款间极端并发由原子扣款兜底，失败则整体回滚、申请保持待审）
            walletService.chargeDeposit(joinRequest.getUserId(), duel.getDepositPerMember(), duelId);
            createMembershipTx(duel, joinRequest.getUserId());
            finishRequest(joinRequest, DuelConstant.JOIN_REQUEST_APPROVED,
                    StrUtil.blankToDefault(request.getRemark(), null));
            return null;
        });
        return "已通过，成员入组并扣除押金";
    }

    private void finishRequest(DuelJoinRequest request, int status, String remark) {
        request.setStatus(status);
        request.setReviewRemark(StrUtil.blankToDefault(remark, null));
        request.setReviewTime(new java.util.Date());
        duelJoinRequestMapper.updateById(request);
    }

    @Override
    public DuelVO quit(Long userId, Long duelId) {
        Duel duel = getRequiredDuel(duelId);
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING,
                ErrorCode.OPERATION_ERROR, "死斗已开始，押金锁定不可退出");
        DuelMember member = getMember(duelId, userId);
        ThrowUtils.throwIf(member == null || member.getStatus() == DuelConstant.MEMBER_STATUS_QUIT,
                ErrorCode.NOT_FOUND_ERROR, "你不是该死斗成员");
        transactionTemplate.execute(status -> {
            // 全额退款 + 成员退出 + 影子计划随之下架
            walletService.refund(userId, member.getDeposit(), duelId, "开始前退出，全额退款");
            member.setStatus(DuelConstant.MEMBER_STATUS_QUIT);
            duelMemberMapper.updateById(member);
            checkInPlanService.removeById(member.getPlanId());
            duel.setMemberCount(Math.max(0, duel.getMemberCount() - 1));
            duel.setTotalPool(Math.max(0, duel.getTotalPool() - member.getDeposit()));
            updateById(duel);
            cacheService.evict(CacheService.keyDuelAgg(duelId));
            return null;
        });
        return detail(userId, duelId);
    }

    @Override
    public DuelVO removeMember(Long userId, Long duelId, Long targetUserId) {
        Duel duel = getRequiredDuel(duelId);
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可移除成员");
        ThrowUtils.throwIf(targetUserId.equals(userId),
                ErrorCode.PARAMS_ERROR, "组长不能移除自己，如需退出请先让渡组长");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING
                        && duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "该死斗已结算或解散，无法移除成员");
        DuelMember member = getMember(duelId, targetUserId);
        ThrowUtils.throwIf(member == null || member.getStatus() == DuelConstant.MEMBER_STATUS_QUIT
                        || member.getStatus() == DuelConstant.MEMBER_STATUS_REMOVED,
                ErrorCode.NOT_FOUND_ERROR, "对方不是该死斗的正式成员");

        transactionTemplate.execute(status -> {
            if (duel.getStatus() == DuelConstant.DUEL_STATUS_RECRUITING) {
                // 招募中：语义等同退出，全额退款
                walletService.refund(targetUserId, member.getDeposit(), duelId, "组长移除（招募中），全额退款");
            } else {
                // 进行中：即时结算——退剩余天数份额，缺勤份额入罚没池
                LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
                long elapsed = Math.min(duel.getTotalDays(),
                        java.time.temporal.ChronoUnit.DAYS.between(duel.getStartDate(), today) + 1);
                int refunded = member.getRefunded() == null ? 0 : member.getRefunded();
                int removalRefund = Math.max(0, Math.min(
                        member.getDeposit() * (int) (duel.getTotalDays() - elapsed) / duel.getTotalDays(),
                        member.getDeposit() - refunded));
                int confiscated = member.getDeposit() - refunded - removalRefund;
                walletService.refund(targetUserId, removalRefund, duelId,
                        "组长移除，退还剩余 " + (duel.getTotalDays() - elapsed) + " 天份额");
                duel.setRemovedPool((duel.getRemovedPool() == null ? 0 : duel.getRemovedPool()) + confiscated);
            }
            member.setStatus(DuelConstant.MEMBER_STATUS_REMOVED);
            duelMemberMapper.updateById(member);
            checkInPlanService.removeById(member.getPlanId());
            duel.setMemberCount(Math.max(0, duel.getMemberCount() - 1));
            duel.setTotalPool(Math.max(0, duel.getTotalPool() - member.getDeposit()));
            updateById(duel);
            cacheService.evict(CacheService.keyDuelAgg(duelId));
            return null;
        });
        return detail(userId, duelId);
    }

    @Override
    public DuelVO transfer(Long userId, Long duelId, Long targetUserId) {
        Duel duel = getRequiredDuel(duelId);
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可让渡组长身份");
        ThrowUtils.throwIf(targetUserId.equals(userId), ErrorCode.PARAMS_ERROR, "你已经是组长");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING
                        && duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "该死斗已结算或解散，无法让渡");
        DuelMember target = getMember(duelId, targetUserId);
        ThrowUtils.throwIf(target == null || target.getStatus() == DuelConstant.MEMBER_STATUS_QUIT
                        || target.getStatus() == DuelConstant.MEMBER_STATUS_REMOVED,
                ErrorCode.OPERATION_ERROR, "让渡目标必须是该死斗的正式成员");
        duel.setLeaderId(targetUserId);
        updateById(duel);
        cacheService.evict(CacheService.keyDuelAgg(duelId));
        return detail(userId, duelId);
    }

    @Override
    public DuelVO detail(Long userId, Long duelId) {
        Duel duel = getRequiredDuel(duelId);
        ensureProgressed(duel);
        return buildDuelVO(duel, userId);
    }

    @Override
    public List<DuelVO> listMine(Long userId) {
        List<Duel> duels = new ArrayList<>();
        // 我参与的
        List<Long> joinedIds = duelMemberMapper.selectList(new LambdaQueryWrapper<DuelMember>()
                        .eq(DuelMember::getUserId, userId)
                        .ne(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT))
                .stream().map(DuelMember::getDuelId).toList();
        if (!joinedIds.isEmpty()) {
            duels.addAll(listByIds(joinedIds));
        }
        // 我创建的
        duels.addAll(list(new LambdaQueryWrapper<Duel>()
                .eq(Duel::getLeaderId, userId)
                .orderByDesc(Duel::getCreateTime)));
        Map<Long, Duel> unique = new LinkedHashMap<>();
        for (Duel duel : duels) {
            ensureProgressed(duel);
            unique.putIfAbsent(duel.getId(), duel);
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(Duel::getId).reversed())
                .map(duel -> buildDuelVO(duel, userId))
                .toList();
    }

    // endregion

    // region 开赛

    @Override
    public void settleDueDuels() {
        duelSettlementService.settleAllDue();
    }

    @Override
    public void startDueDuels() {
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        List<Duel> due = list(new LambdaQueryWrapper<Duel>()
                .eq(Duel::getStatus, DuelConstant.DUEL_STATUS_RECRUITING)
                .le(Duel::getStartDate, today));
        for (Duel duel : due) {
            try {
                startIfDue(duel.getId());
            } catch (Exception e) {
                log.error("死斗 {} 开赛失败", duel.getId(), e);
            }
        }
    }

    /**
     * 到点开赛（含不足 2 人自动解散退款）；已开始的直接跳过
     */
    private void startIfDue(Long duelId) {
        transactionTemplate.execute(status -> {
            Duel duel = getById(duelId);
            if (duel == null || duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING) {
                return null;
            }
            LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
            if (duel.getStartDate().isAfter(today)) {
                return null;
            }
            List<DuelMember> members = activeMembers(duelId);
            // 开赛在即：待审加入申请自动拒绝（未获批未扣款，零成本）
            autoRejectPendingApplications(duelId);
            if (members.size() < DuelConstant.MEMBER_MIN) {
                // 人数不足自动解散，全额退款
                for (DuelMember member : members) {
                    walletService.refund(member.getUserId(), member.getDeposit(),
                            duelId, "人数不足自动解散，全额退款");
                    member.setStatus(DuelConstant.MEMBER_STATUS_QUIT);
                    duelMemberMapper.updateById(member);
                    checkInPlanService.removeById(member.getPlanId());
                }
                duel.setStatus(DuelConstant.DUEL_STATUS_DISBANDED);
                duel.setMemberCount(0);
                duel.setTotalPool(0);
                updateById(duel);
                cacheService.evict(CacheService.keyDuelAgg(duelId));
                log.info("死斗 {} 因人数不足（{}人）自动解散", duelId, members.size());
                return null;
            }
            duel.setStatus(DuelConstant.DUEL_STATUS_RUNNING);
            updateById(duel);
            for (DuelMember member : members) {
                member.setStatus(DuelConstant.MEMBER_STATUS_RUNNING);
                duelMemberMapper.updateById(member);
            }
            cacheService.evict(CacheService.keyDuelAgg(duelId));
            log.info("死斗 {} 开赛，{} 名成员参战", duelId, members.size());
            return null;
        });
    }

    /**
     * 详情/列表访问时的 lazy 推进：到点开赛、到点结算（兜底定时任务）
     */
    private void ensureProgressed(Duel duel) {
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        if (duel.getStatus() == DuelConstant.DUEL_STATUS_RECRUITING && !duel.getStartDate().isAfter(today)) {
            startIfDue(duel.getId());
        } else if (duel.getStatus() == DuelConstant.DUEL_STATUS_RUNNING
                && duel.getEndDate().isBefore(today) && duel.getSettled() == 0) {
            duelSettlementService.settleIfDue(duel.getId());
        }
    }

    // endregion

    // region 内部工具

    /**
     * 加入事务：扣押金 + 成员落库
     */
    private void joinDuelTx(Duel duel, Long userId) {
        ThrowUtils.throwIf(duel.getMemberCount() >= DuelConstant.MEMBER_MAX,
                ErrorCode.OPERATION_ERROR, "该死斗已满员（" + DuelConstant.MEMBER_MAX + " 人）");
        walletService.chargeDeposit(userId, duel.getDepositPerMember(), duel.getId());
        createMembershipTx(duel, userId);
    }

    /**
     * 成员落库（影子计划 + 成员记录 + 冗余计数 + 逐出聚合缓存）；扣押金由调用方负责
     */
    private void createMembershipTx(Duel duel, Long userId) {
        ThrowUtils.throwIf(duel.getMemberCount() >= DuelConstant.MEMBER_MAX,
                ErrorCode.OPERATION_ERROR, "该死斗已满员（" + DuelConstant.MEMBER_MAX + " 人）");
        CheckInPlan shadowPlan = checkInPlanService.createShadowPlan(
                userId, duel.getDuelName(), duel.getTotalDays(), duel.getId());
        DuelMember member = new DuelMember();
        member.setDuelId(duel.getId());
        member.setUserId(userId);
        member.setPlanId(shadowPlan.getId());
        member.setDeposit(duel.getDepositPerMember());
        member.setRefunded(0);
        member.setCheckinDays(0);
        member.setStatus(DuelConstant.MEMBER_STATUS_JOINED);
        duelMemberMapper.insert(member);
        duel.setMemberCount(duel.getMemberCount() + 1);
        duel.setTotalPool(duel.getTotalPool() + duel.getDepositPerMember());
        updateById(duel);
        // 成员/奖池已变，逐出聚合缓存
        cacheService.evict(CacheService.keyDuelAgg(duel.getId()));
    }

    /**
     * 待审加入申请自动拒绝（开赛/解散时）
     */
    private void autoRejectPendingApplications(Long duelId) {
        List<DuelJoinRequest> pendings = duelJoinRequestMapper.selectList(
                new LambdaQueryWrapper<DuelJoinRequest>()
                        .eq(DuelJoinRequest::getDuelId, duelId)
                        .eq(DuelJoinRequest::getStatus, DuelConstant.JOIN_REQUEST_PENDING));
        for (DuelJoinRequest request : pendings) {
            finishRequest(request, DuelConstant.JOIN_REQUEST_REJECTED, "死斗开赛，自动拒绝");
        }
    }

    private Duel getRequiredDuel(Long duelId) {
        Duel duel = getById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        return duel;
    }

    private DuelMember getMember(Long duelId, Long userId) {
        return duelMemberMapper.selectOne(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .eq(DuelMember::getUserId, userId));
    }

    private List<DuelMember> activeMembers(Long duelId) {
        return duelMemberMapper.selectList(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .notIn(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT,
                        DuelConstant.MEMBER_STATUS_REMOVED));
    }

    /**
     * 组装详情 VO：观看者无关的聚合层走 Redis（60s TTL + 写时逐出），
     * myRole/myStatus/myPlanId/pendingCount 等个性化字段每次实时拼装——绝不缓存
     */
    private DuelVO buildDuelVO(Duel duel, Long userId) {
        String aggKey = CacheService.keyDuelAgg(duel.getId());
        DuelAggData agg = cacheService.get(aggKey, DuelAggData.class);
        if (agg == null || agg.getDuel() == null) {
            agg = loadAggregate(duel.getId());
            cacheService.put(aggKey, agg, java.time.Duration.ofSeconds(60));
        }
        Duel aggDuel = agg.getDuel();

        DuelVO vo = new DuelVO();
        vo.setId(aggDuel.getId());
        vo.setDuelName(aggDuel.getDuelName());
        vo.setDuelDesc(aggDuel.getDuelDesc());
        vo.setLeaderId(aggDuel.getLeaderId());
        vo.setJoinMode(aggDuel.getJoinMode());
        vo.setDepositPerMember(aggDuel.getDepositPerMember());
        vo.setTotalDays(aggDuel.getTotalDays());
        vo.setStartDate(aggDuel.getStartDate());
        vo.setEndDate(aggDuel.getEndDate());
        vo.setStatus(aggDuel.getStatus());
        vo.setMemberCount(aggDuel.getMemberCount());
        vo.setTotalPool(aggDuel.getTotalPool());
        vo.setSettled(aggDuel.getSettled() != null && aggDuel.getSettled() == 1);

        List<DuelVO.MemberVO> memberVOs = new ArrayList<>(agg.getMembers().size());
        DuelVO.MemberVO myMemberVo = null;
        for (DuelAggData.MemberLine line : agg.getMembers()) {
            DuelVO.MemberVO mv = new DuelVO.MemberVO();
            mv.setUserId(line.getUserId());
            mv.setUserName(line.getUserName());
            mv.setUserAvatar(line.getUserAvatar());
            mv.setIsLeader(line.isLeader());
            mv.setDeposit(line.getDeposit());
            mv.setStatus(line.getStatus());
            mv.setDays(line.getDays());
            memberVOs.add(mv);
            if (line.getUserId().equals(userId)) {
                myMemberVo = mv;
            }
        }
        vo.setMembers(memberVOs);
        if (userId.equals(aggDuel.getLeaderId())) {
            vo.setMyRole("leader");
            vo.setPendingCount(checkInEvidenceMapper.selectCount(new LambdaQueryWrapper<CheckInEvidence>()
                    .eq(CheckInEvidence::getDuelId, duel.getId())
                    .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)));
        } else if (myMemberVo != null) {
            vo.setMyRole("member");
        }
        // 我的影子计划（实时查询，个性化字段）
        DuelMember mine = getMember(duel.getId(), userId);
        if (mine != null && mine.getStatus() != DuelConstant.MEMBER_STATUS_QUIT) {
            vo.setMyStatus(mine.getStatus());
            vo.setMyPlanId(mine.getPlanId());
        }
        return vo;
    }

    /**
     * 从数据库装载观看者无关聚合（挑战 + 成员 + 确认天数）
     */
    private DuelAggData loadAggregate(Long duelId) {
        Duel duel = getById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        List<DuelMember> members = activeMembers(duelId);
        Map<Long, User> users = userMap(members);
        DuelAggData agg = new DuelAggData();
        agg.setDuel(duel);
        List<DuelAggData.MemberLine> lines = new ArrayList<>(members.size());
        for (DuelMember member : members) {
            DuelAggData.MemberLine line = new DuelAggData.MemberLine();
            line.setUserId(member.getUserId());
            User user = users.get(member.getUserId());
            if (user != null) {
                line.setUserName(user.getUserName());
                line.setUserAvatar(user.getUserAvatar());
            }
            line.setLeader(member.getUserId().equals(duel.getLeaderId()));
            line.setDeposit(member.getDeposit());
            line.setStatus(member.getStatus());
            line.setDays(countConfirmedDays(member, duel, today));
            lines.add(line);
        }
        agg.setMembers(lines);
        return agg;
    }

    private Map<Long, User> userMap(List<DuelMember> members) {
        // 直接走 mapper 批量取昵称头像，避免 service 间循环依赖
        List<Long> userIds = members.stream().map(DuelMember::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /**
     * 已确认天数 = 影子计划在 [start_date, min(end_date, today)] 内 status=正常 的记录数
     */
    private int countConfirmedDays(DuelMember member, Duel duel, LocalDate today) {
        LocalDate end = duel.getEndDate().isBefore(today) ? duel.getEndDate() : today;
        if (end.isBefore(duel.getStartDate())) {
            return 0;
        }
        return Math.toIntExact(checkInRecordMapper.selectCount(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getPlanId, member.getPlanId())
                .eq(CheckInRecord::getStatus, CheckInConstant.RECORD_STATUS_NORMAL)
                .ge(CheckInRecord::getCheckInDate, duel.getStartDate())
                .le(CheckInRecord::getCheckInDate, end)));
    }

    // endregion
}
