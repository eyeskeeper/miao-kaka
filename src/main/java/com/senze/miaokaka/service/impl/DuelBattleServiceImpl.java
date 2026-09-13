package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.CheckInEvidenceMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.duel.ReviewRequest;
import com.senze.miaokaka.model.entity.CheckInEvidence;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.ReviewItemVO;
import com.senze.miaokaka.service.CheckInRecordService;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.DuelBattleService;
import com.senze.miaokaka.service.EvidenceAiPreCheckService;
import com.senze.miaokaka.service.StorageService;
import com.senze.miaokaka.service.StoredImage;
import com.senze.miaokaka.service.VisionReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 死斗打卡与凭证审核实现
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

    private final UserMapper userMapper;

    private final StorageService storageService;

    private final VisionReviewService visionReviewService;

    private final CheckInRecordService checkInRecordService;

    private final TransactionTemplate transactionTemplate;

    private final CacheService cacheService;

    private final EvidenceAiPreCheckService evidenceAiPreCheckService;

    // region 打卡

    @Override
    public CheckInResultVO checkIn(Long userId, Long duelId, MultipartFile image, String remark) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RUNNING,
                ErrorCode.OPERATION_ERROR, "死斗不在进行中");
        DuelMember member = getMember(duelId, userId);
        ThrowUtils.throwIf(member == null || member.getStatus() == DuelConstant.MEMBER_STATUS_QUIT,
                ErrorCode.FORBIDDEN_ERROR, "你不是该死斗的正式成员");
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        ThrowUtils.throwIf(today.isBefore(duel.getStartDate()) || today.isAfter(duel.getEndDate()),
                ErrorCode.OPERATION_ERROR, "今日不在挑战期内");

        // 存凭证（原图+服务端压缩预览图，校验格式/大小）
        StoredImage stored = storageService.storeImage(image);

        // 打卡记录（待审核态，唯一键防重复）+ 凭证，同事务
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
            checkInEvidenceMapper.insert(evidence);
            return r;
        });

        // AI 预审（可选）：消费上传时已缓存的结论（无则此刻补审），只写建议不决定结果
        applyAiSuggestion(record.getRecordId(), stored.url());

        // 组长本人打卡：AI 可用则 AI 结论直接生效（公示），否则留在待审由组长自审
        boolean isLeader = duel.getLeaderId().equals(userId);
        if (isLeader) {
            CheckInEvidence evidence = getEvidenceByRecord(record.getRecordId());
            if (visionReviewService.isAvailable() && evidence.getAiSuggestion() != null) {
                boolean approve = evidence.getAiSuggestion() == 0;
                ReviewRequest self = new ReviewRequest();
                self.setRecordId(record.getRecordId());
                self.setApprove(approve);
                self.setReviewRemark("AI 审核：" + StrUtil.blankToDefault(evidence.getAiReason(), "无"));
                return review(userId, duelId, self);
            }
            CheckInResultVO vo = pendingResult(record);
            vo.setEventDesc("凭证已提交。你是组长，请在审核列表中自审（将公示为自审记录）。");
            return vo;
        }
        CheckInResultVO vo = pendingResult(record);
        vo.setEventDesc("凭证已提交，等待组长审核。审核通过的那一刻，你的猫才会出击！");
        return vo;
    }

    // endregion

    // region 审核

    @Override
    public List<ReviewItemVO> pendingList(Long userId, Long duelId) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可查看审核列表");
        List<CheckInEvidence> pendings = checkInEvidenceMapper.selectList(
                new LambdaQueryWrapper<CheckInEvidence>()
                        .eq(CheckInEvidence::getDuelId, duelId)
                        .eq(CheckInEvidence::getReviewStatus, DuelConstant.REVIEW_STATUS_PENDING)
                        .orderByAsc(CheckInEvidence::getId));
        Map<Long, User> users = pendings.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(pendings.stream().map(CheckInEvidence::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
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
    public CheckInResultVO review(Long userId, Long duelId, ReviewRequest request) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(!duel.getLeaderId().equals(userId), ErrorCode.NO_AUTH_ERROR, "仅组长可审核");
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

            // 是否组长自审自己的卡（公示标记）
            boolean selfReview = record.getUserId().equals(userId);
            evidence.setReviewerId(userId);
            evidence.setIsSelfReview(selfReview ? 1 : 0);
            evidence.setReviewTime(new java.util.Date());
            evidence.setReviewRemark(StrUtil.blankToDefault(request.getReviewRemark(), null));
            if (approve) {
                evidence.setReviewStatus(DuelConstant.REVIEW_STATUS_APPROVED);
                checkInEvidenceMapper.updateById(evidence);
                // 记录状态流转（待审核→正常）由结算方法内部完成并校验
                return checkInRecordService.settleApprovedCheckIn(
                        record.getUserId(), record.getPlanId(), record.getRecordId());
            }
            evidence.setReviewStatus(DuelConstant.REVIEW_STATUS_REJECTED);
            checkInEvidenceMapper.updateById(evidence);
            record.setStatus(CheckInConstant.RECORD_STATUS_ABNORMAL);
            checkInRecordService.updateById(record);
            // 审核改变成员确认天数（通过+1），逐出死斗聚合缓存
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

    // region 内部工具

    /**
     * 消费上传时已缓存的 AI 预审结论写入凭证（上传时由 EvidenceAiPreCheckService 预审并缓存；
     * 若无缓存——如历史图片——则此处补审一次）。开关关闭时为空操作。
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
