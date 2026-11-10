package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.constant.NotificationConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.CheckInEvidenceMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.mapper.DuelReportMapper;
import com.senze.miaokaka.mapper.EvidenceAppealMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.duel.AppealResolveRequest;
import com.senze.miaokaka.model.dto.duel.AppealSubmitRequest;
import com.senze.miaokaka.model.dto.duel.ReportResolveRequest;
import com.senze.miaokaka.model.dto.duel.ReportSubmitRequest;
import com.senze.miaokaka.model.entity.CheckInEvidence;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.model.entity.DuelReport;
import com.senze.miaokaka.model.entity.EvidenceAppeal;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.AppealPendingVO;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.ReportAdminVO;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.CheckInRecordService;
import com.senze.miaokaka.service.DuelAppealService;
import com.senze.miaokaka.service.NotificationService;
import com.senze.miaokaka.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 凭证上诉与死斗举报实现。
 * 救济分层：互审局驳回 → 上诉（组长仲裁，超 48h 未仲裁自动改判）
 * → 维持后可举报（admin 终裁）；组长局驳回 → 直接举报（admin 终裁）。
 * 改判通过 = 完全等价正常审核通过（补钱/恢复记录/回溯连击/补发事件结算）。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelAppealServiceImpl implements DuelAppealService {

    /**
     * 上诉窗口与组长仲裁时限：驳回后 48 小时
     */
    private static final long APPEAL_WINDOW_HOURS = 48;

    private static final long PAGE_SIZE = 20;

    private final EvidenceAppealMapper evidenceAppealMapper;

    private final DuelReportMapper duelReportMapper;

    private final CheckInEvidenceMapper checkInEvidenceMapper;

    private final CheckInRecordMapper checkInRecordMapper;

    private final DuelMapper duelMapper;

    private final DuelMemberMapper duelMemberMapper;

    private final UserMapper userMapper;

    private final CheckInRecordService checkInRecordService;

    private final WalletService walletService;

    private final NotificationService notificationService;

    private final TransactionTemplate transactionTemplate;

    private final CacheService cacheService;

    // region 上诉

    @Override
    public Long appeal(Long userId, Long duelId, AppealSubmitRequest request) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "死斗已结束，救济通道已关闭");
        ThrowUtils.throwIf(duel.getReviewMode() == null
                        || duel.getReviewMode() != DuelConstant.REVIEW_MODE_PEER,
                ErrorCode.OPERATION_ERROR, "组长审核局的驳回请直接举报，由平台处理");
        CheckInEvidence evidence = checkInEvidenceMapper.selectById(request.getEvidenceId());
        ThrowUtils.throwIf(evidence == null || !evidence.getDuelId().equals(duelId),
                ErrorCode.NOT_FOUND_ERROR, "凭证不存在");
        ThrowUtils.throwIf(!evidence.getUserId().equals(userId),
                ErrorCode.FORBIDDEN_ERROR, "只能对自己的凭证上诉");
        ThrowUtils.throwIf(evidence.getReviewStatus() != DuelConstant.REVIEW_STATUS_REJECTED,
                ErrorCode.OPERATION_ERROR, "只能对被驳回的凭证上诉");
        ThrowUtils.throwIf(evidence.getReviewTime() == null
                        || System.currentTimeMillis() - evidence.getReviewTime().getTime()
                        > APPEAL_WINDOW_HOURS * 3600 * 1000,
                ErrorCode.OPERATION_ERROR, "驳回已超过 48 小时，无法上诉");
        ThrowUtils.throwIf(!isActiveMember(duelMemberMapper.selectOne(
                        new LambdaQueryWrapper<DuelMember>()
                                .eq(DuelMember::getDuelId, duelId)
                                .eq(DuelMember::getUserId, userId))),
                ErrorCode.FORBIDDEN_ERROR, "你已不在该死斗中，无法上诉");
        EvidenceAppeal appeal = new EvidenceAppeal();
        appeal.setEvidenceId(evidence.getId());
        appeal.setDuelId(duelId);
        appeal.setAppealerId(userId);
        appeal.setReason(request.getReason().trim());
        appeal.setStatus(DuelConstant.APPEAL_STATUS_PENDING);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                evidenceAppealMapper.insert(appeal);
                User submitter = userMapper.selectById(userId);
                notificationService.notify(duel.getLeaderId(), NotificationConstant.TYPE_APPEAL_SUBMITTED,
                        "收到凭证上诉",
                        "「" + duel.getDuelName() + "」成员 " + (submitter == null ? "" : submitter.getUserName())
                                + " 对驳回结果上诉：" + StrUtil.sub(request.getReason(), 0, 60)
                                + "。请在 48 小时内仲裁，逾期自动改判通过。",
                        duelId);
            });
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该凭证已上诉过（每凭证限一次）");
        }
        return appeal.getId();
    }

    @Override
    public List<AppealPendingVO> pendingAppeals(Long userId, Long duelId) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可查看上诉");
        List<EvidenceAppeal> pendings = evidenceAppealMapper.selectList(
                new LambdaQueryWrapper<EvidenceAppeal>()
                        .eq(EvidenceAppeal::getDuelId, duelId)
                        .eq(EvidenceAppeal::getStatus, DuelConstant.APPEAL_STATUS_PENDING)
                        .orderByAsc(EvidenceAppeal::getId));
        return pendings.stream().map(a -> {
            CheckInEvidence evidence = checkInEvidenceMapper.selectById(a.getEvidenceId());
            if (evidence == null) {
                return null;
            }
            CheckInRecord record = checkInRecordMapper.selectById(evidence.getRecordId());
            AppealPendingVO vo = new AppealPendingVO();
            vo.setAppealId(a.getId());
            vo.setEvidenceId(evidence.getId());
            vo.setRecordId(evidence.getRecordId());
            vo.setUserId(evidence.getUserId());
            User submitter = userMapper.selectById(evidence.getUserId());
            if (submitter != null) {
                vo.setUserName(submitter.getUserName());
                vo.setUserAvatar(submitter.getUserAvatar());
            }
            vo.setImageUrl(evidence.getImagePath());
            vo.setPreviewUrl(evidence.getImagePreviewPath());
            vo.setRejectRemark(evidence.getReviewRemark());
            vo.setAppealReason(a.getReason());
            vo.setDeadline(new Date(a.getCreateTime().getTime() + APPEAL_WINDOW_HOURS * 3600 * 1000));
            vo.setCreateTime(a.getCreateTime());
            if (record != null) {
                vo.setCheckInDate(record.getCheckInDate());
                vo.setRemark(record.getRemark());
            }
            return vo;
        }).filter(Objects::nonNull).toList();
    }

    @Override
    public CheckInResultVO resolveAppeal(Long userId, Long duelId, AppealResolveRequest request) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可仲裁上诉");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "死斗已结束，仲裁通道已关闭");
        EvidenceAppeal appeal = evidenceAppealMapper.selectById(request.getAppealId());
        ThrowUtils.throwIf(appeal == null || !appeal.getDuelId().equals(duelId),
                ErrorCode.NOT_FOUND_ERROR, "上诉不存在");
        CheckInEvidence evidence = checkInEvidenceMapper.selectById(appeal.getEvidenceId());
        ThrowUtils.throwIf(evidence == null, ErrorCode.NOT_FOUND_ERROR, "凭证不存在");
        ThrowUtils.throwIf(!isActiveMember(duelMemberMapper.selectOne(
                        new LambdaQueryWrapper<DuelMember>()
                                .eq(DuelMember::getDuelId, duelId)
                                .eq(DuelMember::getUserId, evidence.getUserId()))),
                ErrorCode.OPERATION_ERROR, "上诉人已离局，无法改判（可维持结案）");

        boolean approve = Boolean.TRUE.equals(request.getApprove());
        return transactionTemplate.execute(status -> {
            boolean claimed = claimAppeal(appeal.getId(), approve
                    ? DuelConstant.APPEAL_STATUS_OVERTURNED : DuelConstant.APPEAL_STATUS_UPHELD,
                    userId, request.getRemark());
            ThrowUtils.throwIf(!claimed, ErrorCode.OPERATION_ERROR, "该上诉已被处理过");
            if (!approve) {
                notificationService.notify(appeal.getAppealerId(), NotificationConstant.TYPE_APPEAL_RESOLVED,
                        "上诉维持驳回",
                        "「" + duel.getDuelName() + "」组长维持驳回："
                                + StrUtil.blankToDefault(request.getRemark(), "维持原判")
                                + "。如仍不服可举报死斗，由平台终裁。",
                        duelId);
                return null;
            }
            CheckInResultVO vo = applyReversal(duel, evidence, userId,
                    "组长仲裁改判通过：" + StrUtil.blankToDefault(request.getRemark(), "无"));
            notificationService.notify(appeal.getAppealerId(), NotificationConstant.TYPE_APPEAL_RESOLVED,
                    "上诉改判通过",
                    "「" + duel.getDuelName() + "」组长改判通过，当日份额已补回，事件结算已补发。",
                    duelId);
            return vo;
        });
    }

    @Override
    public int resolveOverdue() {
        Date cutoff = new Date(System.currentTimeMillis() - APPEAL_WINDOW_HOURS * 3600 * 1000);
        List<EvidenceAppeal> overdue = evidenceAppealMapper.selectList(
                new LambdaQueryWrapper<EvidenceAppeal>()
                        .eq(EvidenceAppeal::getStatus, DuelConstant.APPEAL_STATUS_PENDING)
                        .lt(EvidenceAppeal::getCreateTime, cutoff)
                        .orderByAsc(EvidenceAppeal::getId)
                        .last("limit 50"));
        int resolved = 0;
        for (EvidenceAppeal appeal : overdue) {
            Duel duel = duelMapper.selectById(appeal.getDuelId());
            // 非进行中局的未决上诉由结算/解散流程统一置失效，此处不碰
            if (duel == null || duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING) {
                continue;
            }
            CheckInEvidence evidence = checkInEvidenceMapper.selectById(appeal.getEvidenceId());
            if (evidence == null || evidence.getReviewStatus() != DuelConstant.REVIEW_STATUS_REJECTED) {
                continue;
            }
            CheckInResultVO settled = transactionTemplate.execute(status -> {
                boolean claimed = claimAppeal(appeal.getId(), DuelConstant.APPEAL_STATUS_TIMEOUT_OVERTURNED,
                        null, "组长 48 小时未仲裁，自动改判通过");
                if (!claimed) {
                    return null;
                }
                return applyReversal(duel, evidence, null, "组长 48 小时未仲裁，自动改判通过");
            });
            if (settled != null) {
                notificationService.notify(appeal.getAppealerId(), NotificationConstant.TYPE_APPEAL_RESOLVED,
                        "上诉自动改判通过",
                        "「" + duel.getDuelName() + "」组长超 48 小时未仲裁，系统自动改判通过。",
                        duel.getId());
                resolved++;
            }
        }
        if (resolved > 0) {
            log.info("上诉超时自动改判 {} 条", resolved);
        }
        return resolved;
    }

    @Override
    public int invalidatePending(Long duelId) {
        return evidenceAppealMapper.update(null, new LambdaUpdateWrapper<EvidenceAppeal>()
                .eq(EvidenceAppeal::getDuelId, duelId)
                .eq(EvidenceAppeal::getStatus, DuelConstant.APPEAL_STATUS_PENDING)
                .set(EvidenceAppeal::getStatus, DuelConstant.APPEAL_STATUS_UPHELD)
                .set(EvidenceAppeal::getHandleRemark, "死斗已结束，上诉失效（维持原判）")
                .set(EvidenceAppeal::getHandleTime, new Date()));
    }

    // endregion

    // region 举报

    @Override
    public Long report(Long userId, Long duelId, ReportSubmitRequest request) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "死斗已结束，救济通道已关闭");
        boolean peerMode = duel.getReviewMode() != null
                && duel.getReviewMode() == DuelConstant.REVIEW_MODE_PEER;
        CheckInEvidence evidence = checkInEvidenceMapper.selectById(request.getEvidenceId());
        ThrowUtils.throwIf(evidence == null || !evidence.getDuelId().equals(duelId),
                ErrorCode.NOT_FOUND_ERROR, "凭证不存在");
        ThrowUtils.throwIf(!evidence.getUserId().equals(userId),
                ErrorCode.FORBIDDEN_ERROR, "只能举报自己凭证的审核结果");
        ThrowUtils.throwIf(evidence.getReviewStatus() != DuelConstant.REVIEW_STATUS_REJECTED,
                ErrorCode.OPERATION_ERROR, "只能对被驳回的凭证举报");
        if (peerMode) {
            // 互审局：必须先走完组长仲裁且被维持，才能升级举报
            EvidenceAppeal appeal = evidenceAppealMapper.selectOne(
                    new LambdaQueryWrapper<EvidenceAppeal>()
                            .eq(EvidenceAppeal::getEvidenceId, evidence.getId()));
            ThrowUtils.throwIf(appeal == null, ErrorCode.OPERATION_ERROR,
                    "互审局请先上诉，由组长仲裁");
            ThrowUtils.throwIf(appeal.getStatus() != DuelConstant.APPEAL_STATUS_UPHELD,
                    ErrorCode.OPERATION_ERROR, "举报需在上诉被组长维持驳回之后");
        }
        ThrowUtils.throwIf(duelReportMapper.selectCount(new LambdaQueryWrapper<DuelReport>()
                        .eq(DuelReport::getDuelId, duelId)
                        .eq(DuelReport::getReporterId, userId)) > 0,
                ErrorCode.OPERATION_ERROR, "每个死斗限举报一次");
        DuelReport report = new DuelReport();
        report.setDuelId(duelId);
        report.setReporterId(userId);
        report.setEvidenceId(evidence.getId());
        report.setReason(request.getReason().trim());
        report.setStatus(DuelConstant.REPORT_STATUS_PENDING);
        try {
            duelReportMapper.insert(report);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "每个死斗限举报一次");
        }
        // 实时推送给全体 admin（否则要进后台才发现举报积压）
        for (User admin : userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getUserRole, "admin")
                .eq(User::getIsDelete, 0))) {
            notificationService.notify(admin.getId(), NotificationConstant.TYPE_REPORT_SUBMITTED,
                    "收到死斗举报",
                    "「" + duel.getDuelName() + "」成员 " + userNameOf(userId) + " 举报一条被驳回凭证："
                            + StrUtil.sub(request.getReason(), 0, 60) + "。请进管理后台处理。",
                    duelId);
        }
        return report.getId();
    }

    @Override
    public Page<ReportAdminVO> reportPage(long current, long pageSize, Integer status) {
        Page<DuelReport> page = duelReportMapper.selectPage(new Page<>(current, Math.min(pageSize, PAGE_SIZE)),
                new LambdaQueryWrapper<DuelReport>()
                        .eq(status != null, DuelReport::getStatus, status)
                        .orderByAsc(DuelReport::getStatus)
                        .orderByDesc(DuelReport::getId));
        Page<ReportAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toReportVO).filter(Objects::nonNull).toList());
        return voPage;
    }

    @Override
    public CheckInResultVO resolveReport(Long adminId, Long reportId, ReportResolveRequest request) {
        DuelReport report = duelReportMapper.selectById(reportId);
        ThrowUtils.throwIf(report == null, ErrorCode.NOT_FOUND_ERROR, "举报不存在");
        ThrowUtils.throwIf(report.getStatus() != DuelConstant.REPORT_STATUS_PENDING,
                ErrorCode.OPERATION_ERROR, "该举报已处理过");
        Duel duel = duelMapper.selectById(report.getDuelId());
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        CheckInEvidence evidence = checkInEvidenceMapper.selectById(report.getEvidenceId());
        ThrowUtils.throwIf(evidence == null, ErrorCode.NOT_FOUND_ERROR, "凭证不存在");
        boolean approve = Boolean.TRUE.equals(request.getApprove());
        if (!approve) {
            boolean claimed = claimReport(report.getId(), DuelConstant.REPORT_STATUS_UPHELD,
                    adminId, StrUtil.blankToDefault(request.getRemark(), "维持原判"));
            ThrowUtils.throwIf(!claimed, ErrorCode.OPERATION_ERROR, "该举报已被处理");
            notificationService.notify(report.getReporterId(), NotificationConstant.TYPE_APPEAL_RESOLVED,
                    "举报已处理：维持原判",
                    "「" + duel.getDuelName() + "」平台复核后维持原判"
                            + (StrUtil.isBlank(request.getRemark()) ? "" : "：" + request.getRemark()),
                    duel.getId());
            return null;
        }
        // 改判分支：仅进行中局且提交者仍在册（离局者影子计划已下架，无从结算）
        if (duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING) {
            boolean claimed = claimReport(report.getId(), DuelConstant.REPORT_STATUS_UNPROCESSABLE,
                    adminId, "局已结算/解散，无法改判（存档）");
            ThrowUtils.throwIf(!claimed, ErrorCode.OPERATION_ERROR, "该举报已被处理");
            return null;
        }
        if (!isActiveMember(duelMemberMapper.selectOne(
                new LambdaQueryWrapper<DuelMember>()
                        .eq(DuelMember::getDuelId, duel.getId())
                        .eq(DuelMember::getUserId, evidence.getUserId())))) {
            boolean claimed = claimReport(report.getId(), DuelConstant.REPORT_STATUS_UNPROCESSABLE,
                    adminId, "凭证提交者已离局，无法改判（存档）");
            ThrowUtils.throwIf(!claimed, ErrorCode.OPERATION_ERROR, "该举报已被处理");
            return null;
        }
        // 误判留痕：零经济处罚，处理备注记录原审核人；互审局举报改判组长连带
        String misjudge = buildMisjudgeRemark(duel, evidence, request.getRemark());
        CheckInResultVO vo = transactionTemplate.execute(status -> {
            boolean claimed = claimReport(report.getId(), DuelConstant.REPORT_STATUS_OVERTURNED,
                    adminId, misjudge);
            ThrowUtils.throwIf(!claimed, ErrorCode.OPERATION_ERROR, "该举报已被处理");
            return applyReversal(duel, evidence, adminId, "平台终裁改判通过：" + misjudge);
        });
        notificationService.notify(report.getReporterId(), NotificationConstant.TYPE_APPEAL_RESOLVED,
                "举报已处理：改判通过",
                "「" + duel.getDuelName() + "」平台终裁改判通过，当日份额已补回，事件结算已补发。",
                duel.getId());
        return vo;
    }

    // endregion

    // region 内部工具

    /**
     * 改判通过（组长仲裁 / admin 终裁 / 超时自动共用），须在外层事务内调用：
     * 凭证 REJECTED→APPROVED（条件更新抢占，备注记改判链路；reviewerId 保留原判人作误判留痕，
     * overturned 置 1 供误判计数）；
     * 押金局补回被追回的当日份额（钱包退款 + 成员已退计数等额加回）；
     * 记录恢复为正常并补发一次猫事件结算（与正常审核通过完全等价）。
     */
    private CheckInResultVO applyReversal(Duel duel, CheckInEvidence evidence, Long handlerId, String remark) {
        boolean claimed = checkInEvidenceMapper.update(null, new LambdaUpdateWrapper<CheckInEvidence>()
                .eq(CheckInEvidence::getId, evidence.getId())
                .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_REJECTED)
                .set(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_APPROVED)
                .set(CheckInEvidence::getOverturned, 1)
                .set(CheckInEvidence::getReviewTime, new Date())
                .set(CheckInEvidence::getReviewRemark, remark)) == 1;
        ThrowUtils.throwIf(!claimed, ErrorCode.OPERATION_ERROR, "凭证状态已变化，改判失败请刷新");
        CheckInRecord record = checkInRecordMapper.selectById(evidence.getRecordId());
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "打卡记录不存在");
        if (duel.getMode() == null || duel.getMode() == DuelConstant.DUEL_MODE_DEPOSIT) {
            DuelMember member = duelMemberMapper.selectOne(new LambdaQueryWrapper<DuelMember>()
                    .eq(DuelMember::getDuelId, duel.getId())
                    .eq(DuelMember::getUserId, record.getUserId()));
            ThrowUtils.throwIf(member == null, ErrorCode.NOT_FOUND_ERROR, "成员已离局，无法改判");
            int daily = member.getDeposit() / duel.getTotalDays();
            if (daily > 0) {
                walletService.refund(record.getUserId(), daily, duel.getId(), "改判通过，补回当日份额");
                member.setRefunded((member.getRefunded() == null ? 0 : member.getRefunded()) + daily);
                duelMemberMapper.updateById(member);
            }
        }
        CheckInResultVO vo = checkInRecordService.restoreRejectedCheckIn(
                record.getUserId(), record.getPlanId(), record.getRecordId());
        cacheService.evict(CacheService.keyDuelAgg(duel.getId()));
        return vo;
    }

    /**
     * 误判留痕备注：零经济处罚，仅记录。互审局举报改判时原派单审核人与组长双留痕；
     * 组长局改判留痕组长；AI 免审/超时自动通过的驳回不存在人工误判对象。
     */
    private String buildMisjudgeRemark(Duel duel, CheckInEvidence evidence, String adminRemark) {
        String reviewer = evidence.getReviewerId() == null
                ? null : userNameOf(evidence.getReviewerId());
        boolean peerMode = duel.getReviewMode() != null
                && duel.getReviewMode() == DuelConstant.REVIEW_MODE_PEER;
        StringBuilder sb = new StringBuilder();
        if (reviewer != null) {
            sb.append("误判留痕：原审核人 ").append(reviewer).append("；");
            if (peerMode) {
                sb.append("组长 ").append(StrUtil.blankToDefault(userNameOf(duel.getLeaderId()), ""))
                        .append(" 仲裁失职连带；");
            }
        }
        sb.append(StrUtil.blankToDefault(adminRemark, ""));
        return StrUtil.sub(sb.toString(), 0, 255);
    }

    private boolean claimAppeal(Long appealId, int targetStatus, Long handlerId, String remark) {
        return evidenceAppealMapper.update(null, new LambdaUpdateWrapper<EvidenceAppeal>()
                .eq(EvidenceAppeal::getId, appealId)
                .eq(EvidenceAppeal::getStatus, DuelConstant.APPEAL_STATUS_PENDING)
                .set(EvidenceAppeal::getStatus, targetStatus)
                .set(EvidenceAppeal::getHandlerId, handlerId)
                .set(EvidenceAppeal::getHandleRemark, remark)
                .set(EvidenceAppeal::getHandleTime, new Date())) == 1;
    }

    private boolean claimReport(Long reportId, int targetStatus, Long adminId, String remark) {
        return duelReportMapper.update(null, new LambdaUpdateWrapper<DuelReport>()
                .eq(DuelReport::getId, reportId)
                .eq(DuelReport::getStatus, DuelConstant.REPORT_STATUS_PENDING)
                .set(DuelReport::getStatus, targetStatus)
                .set(DuelReport::getAdminId, adminId)
                .set(DuelReport::getHandleRemark, remark)
                .set(DuelReport::getHandleTime, new Date())) == 1;
    }

    private ReportAdminVO toReportVO(DuelReport report) {
        Duel duel = duelMapper.selectById(report.getDuelId());
        if (duel == null) {
            return null;
        }
        CheckInEvidence evidence = checkInEvidenceMapper.selectById(report.getEvidenceId());
        if (evidence == null) {
            return null;
        }
        CheckInRecord record = checkInRecordMapper.selectById(evidence.getRecordId());
        EvidenceAppeal appeal = evidenceAppealMapper.selectOne(
                new LambdaQueryWrapper<EvidenceAppeal>()
                        .eq(EvidenceAppeal::getEvidenceId, evidence.getId()));
        List<Long> userIds = List.of(report.getReporterId(), duel.getLeaderId(), evidence.getUserId(),
                evidence.getReviewerId() == null ? report.getReporterId() : evidence.getReviewerId())
                .stream().distinct().toList();
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        ReportAdminVO vo = new ReportAdminVO();
        vo.setId(report.getId());
        vo.setDuelId(duel.getId());
        vo.setDuelName(duel.getDuelName());
        vo.setDuelStatus(duel.getStatus());
        vo.setPeerReview(duel.getReviewMode() != null
                && duel.getReviewMode() == DuelConstant.REVIEW_MODE_PEER);
        User leader = users.get(duel.getLeaderId());
        vo.setLeaderName(leader == null ? "" : leader.getUserName());
        vo.setReporterId(report.getReporterId());
        User reporter = users.get(report.getReporterId());
        vo.setReporterName(reporter == null ? "" : reporter.getUserName());
        vo.setReason(report.getReason());
        vo.setCreateTime(report.getCreateTime());
        vo.setStatus(report.getStatus());
        vo.setHandleRemark(report.getHandleRemark());
        vo.setHandleTime(report.getHandleTime());
        vo.setEvidenceId(evidence.getId());
        vo.setRecordId(evidence.getRecordId());
        User submitter = users.get(evidence.getUserId());
        vo.setSubmitterName(submitter == null ? "" : submitter.getUserName());
        User reviewer = evidence.getReviewerId() == null ? null : users.get(evidence.getReviewerId());
        vo.setReviewerName(reviewer == null ? "" : reviewer.getUserName());
        vo.setImageUrl(evidence.getImagePath());
        vo.setPreviewUrl(evidence.getImagePreviewPath());
        vo.setRejectRemark(evidence.getReviewRemark());
        if (record != null) {
            vo.setCheckInDate(record.getCheckInDate());
        }
        if (appeal != null) {
            vo.setAppealStatus(appeal.getStatus());
            vo.setAppealReason(appeal.getReason());
            vo.setAppealHandleRemark(appeal.getHandleRemark());
        }
        return vo;
    }

    private boolean isActiveMember(DuelMember member) {
        return member != null && member.getStatus() != DuelConstant.MEMBER_STATUS_QUIT
                && member.getStatus() != DuelConstant.MEMBER_STATUS_REMOVED;
    }

    private String userNameOf(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getUserName();
    }

    // endregion
}
