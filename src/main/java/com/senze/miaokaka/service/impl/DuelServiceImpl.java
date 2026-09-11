package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.mapper.CheckInEvidenceMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.duel.DuelCreateRequest;
import com.senze.miaokaka.model.entity.CheckInEvidence;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.DuelVO;
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

    private final CheckInPlanService checkInPlanService;

    private final CheckInRecordMapper checkInRecordMapper;

    private final CheckInEvidenceMapper checkInEvidenceMapper;

    private final UserMapper userMapper;

    private final WalletService walletService;

    private final DuelSettlementService duelSettlementService;

    private final TransactionTemplate transactionTemplate;

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
            return null;
        });
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
                log.info("死斗 {} 因人数不足（{}人）自动解散", duelId, members.size());
                return null;
            }
            duel.setStatus(DuelConstant.DUEL_STATUS_RUNNING);
            updateById(duel);
            for (DuelMember member : members) {
                member.setStatus(DuelConstant.MEMBER_STATUS_RUNNING);
                duelMemberMapper.updateById(member);
            }
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
     * 加入事务：扣押金 + 影子计划 + 成员记录 + 冗余计数
     */
    private void joinDuelTx(Duel duel, Long userId) {
        ThrowUtils.throwIf(duel.getMemberCount() >= DuelConstant.MEMBER_MAX,
                ErrorCode.OPERATION_ERROR, "该死斗已满员（" + DuelConstant.MEMBER_MAX + " 人）");
        walletService.chargeDeposit(userId, duel.getDepositPerMember(), duel.getId());
        CheckInPlan shadowPlan = checkInPlanService.createShadowPlan(
                userId, duel.getDuelName(), duel.getTotalDays(), duel.getId());
        DuelMember member = new DuelMember();
        member.setDuelId(duel.getId());
        member.setUserId(userId);
        member.setPlanId(shadowPlan.getId());
        member.setDeposit(duel.getDepositPerMember());
        member.setCheckinDays(0);
        member.setStatus(DuelConstant.MEMBER_STATUS_JOINED);
        duelMemberMapper.insert(member);
        duel.setMemberCount(duel.getMemberCount() + 1);
        duel.setTotalPool(duel.getTotalPool() + duel.getDepositPerMember());
        updateById(duel);
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
                .ne(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT));
    }

    private DuelVO buildDuelVO(Duel duel, Long userId) {
        DuelVO vo = new DuelVO();
        vo.setId(duel.getId());
        vo.setDuelName(duel.getDuelName());
        vo.setDuelDesc(duel.getDuelDesc());
        vo.setLeaderId(duel.getLeaderId());
        vo.setDepositPerMember(duel.getDepositPerMember());
        vo.setTotalDays(duel.getTotalDays());
        vo.setStartDate(duel.getStartDate());
        vo.setEndDate(duel.getEndDate());
        vo.setStatus(duel.getStatus());
        vo.setMemberCount(duel.getMemberCount());
        vo.setTotalPool(duel.getTotalPool());
        vo.setSettled(duel.getSettled() != null && duel.getSettled() == 1);

        List<DuelMember> members = activeMembers(duel.getId());
        Map<Long, User> users = userMap(members);
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        DuelVO.MemberVO myMemberVo = null;
        List<DuelVO.MemberVO> memberVOs = new ArrayList<>(members.size());
        for (DuelMember member : members) {
            DuelVO.MemberVO mv = new DuelVO.MemberVO();
            mv.setUserId(member.getUserId());
            mv.setDeposit(member.getDeposit());
            mv.setStatus(member.getStatus());
            mv.setIsLeader(member.getUserId().equals(duel.getLeaderId()));
            User user = users.get(member.getUserId());
            if (user != null) {
                mv.setUserName(user.getUserName());
                mv.setUserAvatar(user.getUserAvatar());
            }
            mv.setDays(countConfirmedDays(member, duel, today));
            memberVOs.add(mv);
            if (member.getUserId().equals(userId)) {
                myMemberVo = mv;
            }
        }
        vo.setMembers(memberVOs);
        if (userId.equals(duel.getLeaderId())) {
            vo.setMyRole("leader");
            vo.setPendingCount(checkInEvidenceMapper.selectCount(new LambdaQueryWrapper<CheckInEvidence>()
                    .eq(CheckInEvidence::getDuelId, duel.getId())
                    .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)));
        } else if (myMemberVo != null) {
            vo.setMyRole("member");
        }
        // 我的影子计划
        DuelMember mine = getMember(duel.getId(), userId);
        if (mine != null && mine.getStatus() != DuelConstant.MEMBER_STATUS_QUIT) {
            vo.setMyStatus(mine.getStatus());
            vo.setMyPlanId(mine.getPlanId());
        }
        return vo;
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
