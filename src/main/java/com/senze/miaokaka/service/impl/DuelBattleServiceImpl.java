package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.constant.NotificationConstant;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.CheckInEvidenceMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelReportMapper;
import com.senze.miaokaka.mapper.EvidenceAppealMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.duel.ReviewRequest;
import com.senze.miaokaka.model.entity.CheckInEvidence;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.model.entity.DuelReport;
import com.senze.miaokaka.model.entity.EvidenceAppeal;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.ReviewHistoryVO;
import com.senze.miaokaka.model.vo.ReviewItemVO;
import com.senze.miaokaka.service.CheckInRecordService;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.DuelBattleService;
import com.senze.miaokaka.service.DuelSettlementService;
import com.senze.miaokaka.service.EvidenceAiPreCheckService;
import com.senze.miaokaka.service.NotificationService;
import com.senze.miaokaka.service.StorageService;
import com.senze.miaokaka.service.StoredImage;
import com.senze.miaokaka.utils.PeerReviewAssigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 死斗打卡与凭证审核实现。
 * 审核方式：组长审核（现状，默认）/ 组内互审（错位环派单，提交时算定 assignee 落库）；
 * AI 预审：局开关开启才生效，建议通过直接免人工，建议驳回转人工。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelBattleServiceImpl extends ServiceImpl<DuelMemberMapper, DuelMember>
        implements DuelBattleService {

    private final DuelMapper duelMapper;

    private final DuelMemberMapper duelMemberMapper;

    private final CheckInEvidenceMapper checkInEvidenceMapper;

    private final EvidenceAppealMapper evidenceAppealMapper;

    private final DuelReportMapper duelReportMapper;

    private final UserMapper userMapper;

    private final StorageService storageService;

    private final CheckInRecordService checkInRecordService;

    private final TransactionTemplate transactionTemplate;

    private final CacheService cacheService;

    private final NotificationService notificationService;

    private final EvidenceAiPreCheckService evidenceAiPreCheckService;

    private final DuelSettlementService duelSettlementService;

    // region 打卡

    @Override
    public CheckInResultVO checkIn(Long userId, Long duelId, MultipartFile image, String remark) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "死斗不在进行中");
        DuelMember member = getMember(duelId, userId);
        ThrowUtils.throwIf(member == null || member.getStatus() == DuelConstant.MEMBER_STATUS_QUIT
                        || member.getStatus() == DuelConstant.MEMBER_STATUS_REMOVED,
                ErrorCode.FORBIDDEN_ERROR, "你不是该死斗的正式成员");
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        ThrowUtils.throwIf(today.isBefore(duel.getStartDate()) || today.isAfter(duel.getEndDate()),
                ErrorCode.OPERATION_ERROR, "今日不在挑战期内");

        // 存凭证（原图+服务端压缩预览图，校验格式/大小）
        StoredImage stored = storageService.storeImage(image);
        boolean peerMode = isPeerMode(duel);
        // lambda 内带回昨日分成（0=无资格/无池）
        int[] yesterdayShare = {0};

        // 打卡记录（待审核态，唯一键防重复）+ 凭证 + 当日奖励即发，同事务：
        // 打卡立刻拿当日份额，审核驳回再追回（用户体验优先的激励时序）
        CheckInRecord record = transactionTemplate.execute(status -> {
            CheckInRecord r = new CheckInRecord();
            r.setUserId(userId);
            r.setPlanId(member.getPlanId());
            r.setCheckInDate(today);
            r.setCheckInTime(new java.util.Date());
            r.setStatus(CheckInConstant.RECORD_STATUS_PENDING);
            r.setRemark(StrUtil.blankToDefault(remark, null));
            try {
                checkInRecordService.save(r);
            } catch (DuplicateKeyException e) {
                throw new com.senze.miaokaka.exception.BusinessException(
                        ErrorCode.OPERATION_ERROR, "今天已经提交过打卡凭证啦");
            }
            CheckInEvidence evidence = new CheckInEvidence();
            evidence.setRecordId(r.getRecordId());
            evidence.setDuelId(duelId);
            evidence.setUserId(userId);
            evidence.setImagePath(stored.url());
            evidence.setImagePreviewPath(stored.previewUrl());
            evidence.setReviewStatus(DuelConstant.REVIEW_STATUS_PENDING);
            evidence.setIsSelfReview(0);
            // 组内互审：按提交日错位环算定派单审核员（组长同样被派单；进行中局恒 ≥2 人）
            if (peerMode) {
                DuelMember assignee = PeerReviewAssigner.assigneeOf(activeMembers(duelId), userId,
                        ChronoUnit.DAYS.between(duel.getStartDate(), today));
                evidence.setAssigneeId(assignee == null ? null : assignee.getUserId());
            }
            checkInEvidenceMapper.insert(evidence);
            // 当日奖励即发 + 昨日缺卡分成（昨日有效打卡者才有资格领）
            duelSettlementService.dailyRefund(duel, member);
            yesterdayShare[0] = duelSettlementService.shareYesterdayPool(duel, member);
            return r;
        });

        // AI 预审：局开关开启才消费上传缓存（无则此刻补审）；关闭时不产生任何建议
        if (duel.getAiReview() != null && duel.getAiReview() == 1) {
            applyAiSuggestion(record.getRecordId(), stored.url());
        }

        CheckInEvidence evidence = getEvidenceByRecord(record.getRecordId());

        // 组内互审：派单到达推送（审核员不用进详情自己发现）
        if (peerMode && evidence.getAssigneeId() != null) {
            User submitter = userMapper.selectById(userId);
            notificationService.notify(evidence.getAssigneeId(), NotificationConstant.TYPE_REVIEW_ASSIGNED,
                    "待你审核的凭证",
                    "「" + duel.getDuelName() + "」" + (submitter == null ? "成员" : submitter.getUserName())
                            + " 提交了今日打卡凭证，今日由你审核；当日未审完将自动通过。",
                    duelId);
        }

        // AI 建议通过 → 免人工直接通过（两种审核方式一致；AI 不可用/建议驳回/局未开启 → 留待人工）
        if (evidence.getAiSuggestion() != null && evidence.getAiSuggestion() == 0) {
            CheckInResultVO settled = autoApproveEvidence(evidence,
                    "AI 预审通过：" + StrUtil.blankToDefault(evidence.getAiReason(), "无"));
            if (settled != null) {
                if (yesterdayShare[0] > 0) {
                    settled.setCoinReward((settled.getCoinReward() == null ? 0 : settled.getCoinReward()) + yesterdayShare[0]);
                    settled.setEventDesc(StrUtil.blankToDefault(settled.getEventDesc(), "")
                            + " 昨日缺卡分成 +" + yesterdayShare[0] + " 喵币。");
                }
                return settled;
            }
        }

        CheckInResultVO vo = pendingResult(record);
        String shareNote = yesterdayShare[0] > 0 ? " 昨日缺卡分成 +" + yesterdayShare[0] + " 喵币已到账。" : "";
        if (yesterdayShare[0] > 0) {
            vo.setCoinReward(yesterdayShare[0]);
        }
        if (peerMode) {
            String assigneeName = userNameOf(evidence.getAssigneeId());
            vo.setEventDesc((assigneeName == null
                    ? "凭证已提交，等待审核；当日奖励已到账，若被驳回将追回。"
                    : "凭证已提交，等待「" + assigneeName + "」审核；当日奖励已到账，若被驳回将追回。") + shareNote);
        } else if (duel.getLeaderId().equals(userId)) {
            vo.setEventDesc("凭证已提交。你是组长，请在审核列表中自审（将公示为自审记录）。" + shareNote);
        } else {
            vo.setEventDesc("凭证已提交，当日奖励已到账；若审核被驳回将追回当日奖励。" + shareNote);
        }
        return vo;
    }

    // endregion

    // region 审核

    @Override
    public List<ReviewItemVO> pendingList(Long userId, Long duelId, String scope) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        boolean leader = duel.getLeaderId().equals(userId);
        boolean peerMode = isPeerMode(duel);
        if (!peerMode) {
            ThrowUtils.throwIf(!leader, ErrorCode.NO_AUTH_ERROR, "仅组长可查看审核列表");
        } else {
            DuelMember member = getMember(duelId, userId);
            ThrowUtils.throwIf(!leader && !isActiveMember(member),
                    ErrorCode.NO_AUTH_ERROR, "仅死斗成员可查看审核列表");
            ThrowUtils.throwIf("all".equals(scope) && !leader,
                    ErrorCode.NO_AUTH_ERROR, "全队待审视图仅组长可看");
        }
        boolean assignedToMe = peerMode && !"all".equals(scope);
        List<CheckInEvidence> pendings = checkInEvidenceMapper.selectList(
                new LambdaQueryWrapper<CheckInEvidence>()
                        .eq(CheckInEvidence::getDuelId, duelId)
                        .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)
                        .eq(assignedToMe, CheckInEvidence::getAssigneeId, userId)
                        .orderByAsc(CheckInEvidence::getId));
        Map<Long, User> users = pendings.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(pendings.stream().map(CheckInEvidence::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, String> assigneeNames = peerMode
                ? userNamesOf(pendings.stream().map(CheckInEvidence::getAssigneeId))
                : Map.of();
        Map<Long, CheckInRecord> records = pendings.isEmpty() ? Map.of()
                : checkInRecordService.listByIds(pendings.stream().map(CheckInEvidence::getRecordId).toList())
                .stream().collect(Collectors.toMap(CheckInRecord::getRecordId, Function.identity()));
        return pendings.stream().map(e -> {
            ReviewItemVO vo = new ReviewItemVO();
            vo.setRecordId(e.getRecordId());
            vo.setUserId(e.getUserId());
            User user = users.get(e.getUserId());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
            vo.setAssigneeName(e.getAssigneeId() == null ? null
                    : assigneeNames.get(e.getAssigneeId()));
            vo.setImageUrl(e.getImagePath());
            vo.setPreviewUrl(e.getImagePreviewPath());
            vo.setAiSuggestion(e.getAiSuggestion());
            vo.setAiReason(e.getAiReason());
            CheckInRecord record = records.get(e.getRecordId());
            if (record != null) {
                vo.setCheckInDate(record.getCheckInDate());
                vo.setRemark(record.getRemark());
            }
            return vo;
        }).toList();
    }

    @Override
    public List<ReviewHistoryVO> reviewHistory(Long userId, Long duelId) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        // 组内公示：组长或在册成员可见（已退出/被移除者不可再看）
        DuelMember member = getMember(duelId, userId);
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId) && !isActiveMember(member),
                ErrorCode.NO_AUTH_ERROR, "仅死斗成员可查看审核记录");
        List<CheckInEvidence> rows = checkInEvidenceMapper.selectList(
                new LambdaQueryWrapper<CheckInEvidence>()
                        .eq(CheckInEvidence::getDuelId, duelId)
                        .in(CheckInEvidence::getReviewStatus,
                                DuelConstant.REVIEW_STATUS_APPROVED, DuelConstant.REVIEW_STATUS_REJECTED)
                        .orderByDesc(CheckInEvidence::getReviewTime)
                        .last("limit 50"));
        // 一次批量取全：提交者 + 审核人 + 派单人
        List<Long> userIds = new ArrayList<>();
        rows.forEach(e -> {
            userIds.add(e.getUserId());
            if (e.getReviewerId() != null) {
                userIds.add(e.getReviewerId());
            }
            if (e.getAssigneeId() != null) {
                userIds.add(e.getAssigneeId());
            }
        });
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds.stream().distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, CheckInRecord> records = rows.isEmpty() ? Map.of()
                : checkInRecordService.listByIds(rows.stream().map(CheckInEvidence::getRecordId).toList())
                .stream().collect(Collectors.toMap(CheckInRecord::getRecordId, Function.identity()));
        // 上诉联查（每凭证至多一条），改判/维持状态随审核记录公示
        Map<Long, EvidenceAppeal> appeals = rows.isEmpty() ? Map.of()
                : evidenceAppealMapper.selectList(new LambdaQueryWrapper<EvidenceAppeal>()
                        .eq(EvidenceAppeal::getDuelId, duelId)
                        .in(EvidenceAppeal::getEvidenceId,
                                rows.stream().map(CheckInEvidence::getId).toList()))
                .stream().collect(Collectors.toMap(EvidenceAppeal::getEvidenceId, Function.identity()));
        // 举报联查：组长局的改判走举报通道（无上诉记录）， overturned 标记需覆盖
        Map<Long, DuelReport> reports = rows.isEmpty() ? Map.of()
                : duelReportMapper.selectList(new LambdaQueryWrapper<DuelReport>()
                        .eq(DuelReport::getDuelId, duelId)
                        .in(DuelReport::getEvidenceId,
                                rows.stream().map(CheckInEvidence::getId).toList()))
                .stream().collect(Collectors.toMap(DuelReport::getEvidenceId, Function.identity(), (a, b) -> a));
        return rows.stream().map(e -> {
            ReviewHistoryVO vo = new ReviewHistoryVO();
            vo.setRecordId(e.getRecordId());
            vo.setEvidenceId(e.getId());
            vo.setUserId(e.getUserId());
            User submitter = users.get(e.getUserId());
            if (submitter != null) {
                vo.setUserName(submitter.getUserName());
            }
            User reviewer = e.getReviewerId() == null ? null : users.get(e.getReviewerId());
            vo.setReviewerName(reviewer == null ? null : reviewer.getUserName());
            User assignee = e.getAssigneeId() == null ? null : users.get(e.getAssigneeId());
            vo.setAssigneeName(assignee == null ? null : assignee.getUserName());
            vo.setReviewStatus(e.getReviewStatus());
            vo.setSelfReview(e.getIsSelfReview() != null && e.getIsSelfReview() == 1);
            vo.setReviewRemark(e.getReviewRemark());
            vo.setAiSuggestion(e.getAiSuggestion());
            vo.setAiReason(e.getAiReason());
            vo.setReviewTime(e.getReviewTime());
            EvidenceAppeal appeal = appeals.get(e.getId());
            if (appeal != null) {
                vo.setAppealStatus(appeal.getStatus());
                vo.setAppealReason(appeal.getReason());
            }
            DuelReport report = reports.get(e.getId());
            boolean overturned = (appeal != null && (appeal.getStatus() == DuelConstant.APPEAL_STATUS_OVERTURNED
                    || appeal.getStatus() == DuelConstant.APPEAL_STATUS_TIMEOUT_OVERTURNED))
                    || (report != null && report.getStatus() == DuelConstant.REPORT_STATUS_OVERTURNED);
            vo.setOverturned(overturned);
            CheckInRecord record = records.get(e.getRecordId());
            if (record != null) {
                vo.setCheckInDate(record.getCheckInDate());
            }
            return vo;
        }).toList();
    }

    @Override
    public CheckInResultVO review(Long userId, Long duelId, ReviewRequest request) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        boolean peerMode = isPeerMode(duel);
        if (!peerMode) {
            ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可审核");
        }
        boolean approve = Boolean.TRUE.equals(request.getApprove());
        ThrowUtils.throwIf(!approve && StrUtil.isBlank(request.getReviewRemark()),
                ErrorCode.PARAMS_ERROR, "驳回时必须填写理由");

        CheckInResultVO result = transactionTemplate.execute(status -> {
            CheckInRecord record = checkInRecordService.getById(request.getRecordId());
            ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "打卡记录不存在");
            CheckInEvidence evidence = getEvidenceByRecord(record.getRecordId());
            ThrowUtils.throwIf(evidence == null || !evidence.getDuelId().equals(duelId),
                    ErrorCode.NOT_FOUND_ERROR, "凭证不存在");
            ThrowUtils.throwIf(evidence.getReviewStatus() != DuelConstant.REVIEW_STATUS_PENDING,
                    ErrorCode.OPERATION_ERROR, "该凭证已审核过");
            ThrowUtils.throwIf(record.getStatus() != CheckInConstant.RECORD_STATUS_PENDING,
                    ErrorCode.OPERATION_ERROR, "该记录不在待审核状态");
            // 组内互审：只有派单审核人可终审（组长也不例外；派单为空时由 24h 超时兜底）
            if (peerMode) {
                ThrowUtils.throwIf(!userId.equals(evidence.getAssigneeId()),
                        ErrorCode.NO_AUTH_ERROR, "该凭证已派给其他成员审核");
            }

            // 是否审核人自审自己的卡（公示标记；组长审核局的组长自审）
            boolean selfReview = record.getUserId().equals(userId);
            evidence.setReviewerId(userId);
            evidence.setIsSelfReview(selfReview ? 1 : 0);
            evidence.setReviewTime(new java.util.Date());
            evidence.setReviewRemark(StrUtil.blankToDefault(request.getReviewRemark(), null));
            if (approve) {
                evidence.setReviewStatus(DuelConstant.REVIEW_STATUS_APPROVED);
                checkInEvidenceMapper.updateById(evidence);
                // 记录状态流转（待审核→正常）由结算方法内部完成并校验；
                // 当日奖励已在打卡时即发，通过无需再处理
                return checkInRecordService.settleApprovedCheckIn(
                        record.getUserId(), record.getPlanId(), record.getRecordId());
            }
            evidence.setReviewStatus(DuelConstant.REVIEW_STATUS_REJECTED);
            checkInEvidenceMapper.updateById(evidence);
            record.setStatus(CheckInConstant.RECORD_STATUS_ABNORMAL);
            checkInRecordService.updateById(record);
            // 驳回：追回打卡时已发放的当日奖励（允许余额临时为负，保证账目精确），
            // 同时审核改变成员确认天数，逐出死斗聚合缓存
            DuelMember rejectedMember = getMember(duelId, record.getUserId());
            if (rejectedMember != null) {
                duelSettlementService.clawbackDaily(duel, rejectedMember);
            }
            cacheService.evict(CacheService.keyDuelAgg(duelId));
            return null;
        });

        if (result == null) {
            CheckInRecord record = checkInRecordService.getById(request.getRecordId());
            CheckInResultVO vo = new CheckInResultVO();
            vo.setEventType("rejected");
            vo.setEventDesc("凭证已被驳回：" + StrUtil.blankToDefault(request.getReviewRemark(), "无理由")
                    + "。当日按缺卡处理，押金占比将被没收。");
            vo.setCatName("");
            vo.setCheckInDate(record == null ? null : record.getCheckInDate());
            return vo;
        }
        return result;
    }

    // endregion

    // region 超时兜底

    @Override
    public int autoApproveOverdue() {
        // SLA 当日化：今日 00:00 之前创建的待审凭证全部自动通过——
        // 任何凭证活不过提交当天，昨日出勤名单次日完全不可变（每日缺卡分成的稳定前提）
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        java.util.Date cutoff = java.util.Date.from(today.atStartOfDay(CheckInConstant.BIZ_ZONE).toInstant());
        // 分批扫：每轮最多 200 条，防极端积压拖长事务
        List<CheckInEvidence> overdue = checkInEvidenceMapper.selectList(
                new LambdaQueryWrapper<CheckInEvidence>()
                        .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)
                        .lt(CheckInEvidence::getCreateTime, cutoff)
                        .orderByAsc(CheckInEvidence::getId)
                        .last("limit 200"));
        int approved = 0;
        for (CheckInEvidence evidence : overdue) {
            // 仅处理进行中局：招募局没有凭证，已结算/已解散局由结算与解散流程兜底
            Duel duel = duelMapper.selectById(evidence.getDuelId());
            if (duel == null || duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING) {
                continue;
            }
            CheckInResultVO settled = autoApproveEvidence(evidence, "当日未审完，自动通过");
            if (settled != null) {
                approved++;
            }
        }
        if (approved > 0) {
            log.info("当日未审完自动通过 {} 条待审凭证", approved);
        }
        return approved;
    }

    @Override
    public int reassignPending(Duel duel, Long removedUserId) {
        List<CheckInEvidence> pendings = checkInEvidenceMapper.selectList(
                new LambdaQueryWrapper<CheckInEvidence>()
                        .eq(CheckInEvidence::getDuelId, duel.getId())
                        .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)
                        .eq(CheckInEvidence::getAssigneeId, removedUserId));
        if (pendings.isEmpty()) {
            return 0;
        }
        Map<Long, CheckInRecord> records = checkInRecordService.listByIds(
                        pendings.stream().map(CheckInEvidence::getRecordId).toList())
                .stream().collect(Collectors.toMap(CheckInRecord::getRecordId, Function.identity()));
        int reassigned = 0;
        for (CheckInEvidence evidence : pendings) {
            CheckInRecord record = records.get(evidence.getRecordId());
            LocalDate submitDate = record == null
                    ? LocalDate.now(CheckInConstant.BIZ_ZONE) : record.getCheckInDate();
            // 每条重新取在册成员：assigneeOf 会就地排序，且被移除者已不在其中
            DuelMember assignee = PeerReviewAssigner.assigneeOf(activeMembers(duel.getId()),
                    evidence.getUserId(), ChronoUnit.DAYS.between(duel.getStartDate(), submitDate));
            checkInEvidenceMapper.update(null, new LambdaUpdateWrapper<CheckInEvidence>()
                    .eq(CheckInEvidence::getId, evidence.getId())
                    .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)
                    .set(CheckInEvidence::getAssigneeId, assignee == null ? null : assignee.getUserId()));
            reassigned++;
        }
        log.info("死斗 {} 移除审核员 {}，重派在途待审凭证 {} 条", duel.getId(), removedUserId, reassigned);
        return reassigned;
    }

    @Override
    public int approveAllPending(Long duelId, String remark) {
        List<CheckInEvidence> pendings = checkInEvidenceMapper.selectList(
                new LambdaQueryWrapper<CheckInEvidence>()
                        .eq(CheckInEvidence::getDuelId, duelId)
                        .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING));
        int approved = 0;
        for (CheckInEvidence evidence : pendings) {
            if (autoApproveEvidence(evidence, remark) != null) {
                approved++;
            }
        }
        return approved;
    }

    @Override
    public int closePendingOfUser(Long duelId, Long userId, String remark) {
        List<CheckInEvidence> pendings = checkInEvidenceMapper.selectList(
                new LambdaQueryWrapper<CheckInEvidence>()
                        .eq(CheckInEvidence::getDuelId, duelId)
                        .eq(CheckInEvidence::getUserId, userId)
                        .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING));
        int closed = 0;
        for (CheckInEvidence evidence : pendings) {
            boolean claimed = checkInEvidenceMapper.update(null, new LambdaUpdateWrapper<CheckInEvidence>()
                    .eq(CheckInEvidence::getId, evidence.getId())
                    .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)
                    .set(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_APPROVED)
                    .set(CheckInEvidence::getReviewerId, null)
                    .set(CheckInEvidence::getReviewTime, new java.util.Date())
                    .set(CheckInEvidence::getReviewRemark, remark)) == 1;
            if (!claimed) {
                continue;
            }
            CheckInRecord record = checkInRecordService.getById(evidence.getRecordId());
            if (record != null && record.getStatus() == CheckInConstant.RECORD_STATUS_PENDING) {
                record.setStatus(CheckInConstant.RECORD_STATUS_NORMAL);
                checkInRecordService.updateById(record);
            }
            closed++;
        }
        return closed;
    }

    // endregion

    // region 内部工具

    /**
     * 自动通过一条待审凭证（条件更新抢占，防与人工审核并发重复）：
     * evidence→APPROVED（reviewerId 置空标记非人工，assigneeId 保留派单留痕），
     * 记录状态流转与事件结算复用人工审核通过的结算路径。
     * 返回结算结果；凭证已被审掉或记录不在待审态时返回 null。
     */
    private CheckInResultVO autoApproveEvidence(CheckInEvidence evidence, String remark) {
        boolean claimed = checkInEvidenceMapper.update(null, new LambdaUpdateWrapper<CheckInEvidence>()
                .eq(CheckInEvidence::getId, evidence.getId())
                .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)
                .set(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_APPROVED)
                .set(CheckInEvidence::getReviewerId, null)
                .set(CheckInEvidence::getReviewTime, new java.util.Date())
                .set(CheckInEvidence::getReviewRemark, remark)) == 1;
        if (!claimed) {
            return null;
        }
        CheckInRecord record = checkInRecordService.getById(evidence.getRecordId());
        if (record == null || record.getStatus() != CheckInConstant.RECORD_STATUS_PENDING) {
            return null;
        }
        return checkInRecordService.settleApprovedCheckIn(
                record.getUserId(), record.getPlanId(), record.getRecordId());
    }

    /**
     * 消费上传时已缓存的 AI 预审结论写入凭证（上传时由 EvidenceAiPreCheckService 预审并缓存；
     * 若无缓存——如历史图片——则此处补审一次）。仅局开启 AI 预审时调用。
     */
    private void applyAiSuggestion(Long recordId, String imageUrl) {
        evidenceAiPreCheckService.preCheck(imageUrl);
        evidenceAiPreCheckService.consume(imageUrl).ifPresent(verdict -> {
            CheckInEvidence evidence = getEvidenceByRecord(recordId);
            if (evidence != null) {
                evidence.setAiSuggestion(verdict.suggestApprove() ? 0 : 1);
                evidence.setAiReason(StrUtil.blankToDefault(verdict.reason(), null));
                checkInEvidenceMapper.updateById(evidence);
            }
        });
    }

    private boolean isPeerMode(Duel duel) {
        return duel.getReviewMode() != null
                && duel.getReviewMode() == DuelConstant.REVIEW_MODE_PEER;
    }

    private boolean isActiveMember(DuelMember member) {
        return member != null && member.getStatus() != DuelConstant.MEMBER_STATUS_QUIT
                && member.getStatus() != DuelConstant.MEMBER_STATUS_REMOVED;
    }

    private List<DuelMember> activeMembers(Long duelId) {
        return duelMemberMapper.selectList(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .notIn(DuelMember::getStatus,
                        DuelConstant.MEMBER_STATUS_QUIT, DuelConstant.MEMBER_STATUS_REMOVED));
    }

    private String userNameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getUserName();
    }

    private Map<Long, String> userNamesOf(java.util.stream.Stream<Long> ids) {
        List<Long> distinct = ids.filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(User::getId, User::getUserName));
    }

    private CheckInEvidence getEvidenceByRecord(Long recordId) {
        return checkInEvidenceMapper.selectOne(new LambdaQueryWrapper<CheckInEvidence>()
                .eq(CheckInEvidence::getRecordId, recordId));
    }

    private DuelMember getMember(Long duelId, Long userId) {
        return duelMemberMapper.selectOne(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .eq(DuelMember::getUserId, userId));
    }

    private CheckInResultVO pendingResult(CheckInRecord record) {
        CheckInResultVO vo = new CheckInResultVO();
        vo.setEventType("pending");
        vo.setCheckInDate(record.getCheckInDate());
        vo.setRemark(record.getRemark());
        vo.setLevelUp(false);
        vo.setBossDefeated(false);
        return vo;
    }

    // endregion
}
