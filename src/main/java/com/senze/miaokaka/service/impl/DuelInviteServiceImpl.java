package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.DuelInviteMapper;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.DuelMemberMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.DuelInvite;
import com.senze.miaokaka.model.entity.DuelMember;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.InviteInfoVO;
import com.senze.miaokaka.model.vo.InviteUseResultVO;
import com.senze.miaokaka.model.vo.InviteVO;
import com.senze.miaokaka.service.DuelInviteService;
import com.senze.miaokaka.service.DuelService;
import com.senze.miaokaka.service.StorageService;
import com.senze.miaokaka.utils.PosterRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 死斗邀请服务实现。
 * 规则：仅招募中可生成/使用；组长码在自由制与审批制下都直接入组（审批制免审）；
 * 成员码：自由制直接加入、审批制提交申请并留痕邀请人；
 * 每个（成员，死斗）固定一个长期码，海报只渲染一次复用。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelInviteServiceImpl extends ServiceImpl<DuelInviteMapper, DuelInvite>
        implements DuelInviteService {

    /**
     * 邀请码字符集：剔除 0/O/1/I 易混字符
     */
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private static final int CODE_LENGTH = 8;

    private static final int CODE_RETRY_MAX = 5;

    private final DuelMapper duelMapper;

    private final DuelMemberMapper duelMemberMapper;

    private final UserMapper userMapper;

    private final DuelService duelService;

    private final StorageService storageService;

    @Value("${app.invite.base-url:http://localhost:18089}")
    private String inviteBaseUrl;

    @Override
    public InviteVO getOrCreateInvite(Long userId, Long duelId) {
        Duel duel = duelMapper.selectById(duelId);
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING,
                ErrorCode.OPERATION_ERROR, "仅招募中的死斗可以生成邀请");
        long joined = duelMemberMapper.selectCount(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duelId)
                .eq(DuelMember::getUserId, userId)
                .ne(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT));
        ThrowUtils.throwIf(joined <= 0, ErrorCode.OPERATION_ERROR, "仅正式成员可以生成邀请");

        DuelInvite invite = getOne(new LambdaQueryWrapper<DuelInvite>()
                .eq(DuelInvite::getDuelId, duelId)
                .eq(DuelInvite::getInviterId, userId));
        if (invite == null) {
            invite = insertWithRetry(duelId, userId);
        }
        if (StrUtil.isBlank(invite.getPosterUrl())) {
            // 海报只渲染一次：失败则本行 poster_url 保持空，下次调用自动重试
            String posterUrl = renderPoster(duel, invite.getCode(), userId);
            invite.setPosterUrl(posterUrl);
            updateById(invite);
        }
        InviteVO vo = new InviteVO();
        vo.setCode(invite.getCode());
        vo.setPosterUrl(invite.getPosterUrl());
        vo.setDuelName(duel.getDuelName());
        vo.setQrContent(qrContentOf(invite.getCode()));
        User inviter = userMapper.selectById(userId);
        vo.setInviterName(inviter == null ? "" : inviter.getUserName());
        return vo;
    }

    @Override
    public InviteInfoVO resolveInfo(String code) {
        DuelInvite invite = requireByCode(code);
        Duel duel = duelMapper.selectById(invite.getDuelId());
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        InviteInfoVO vo = new InviteInfoVO();
        vo.setDuelId(duel.getId());
        vo.setDuelName(duel.getDuelName());
        User leader = userMapper.selectById(duel.getLeaderId());
        vo.setLeaderName(leader == null ? "" : leader.getUserName());
        vo.setJoinMode(duel.getJoinMode());
        vo.setDepositPerMember(duel.getDepositPerMember());
        vo.setTotalDays(duel.getTotalDays());
        vo.setMemberCount(duel.getMemberCount());
        vo.setStatus(duel.getStatus());
        User inviter = userMapper.selectById(invite.getInviterId());
        vo.setInviterName(inviter == null ? "" : inviter.getUserName());
        boolean leaderInvite = invite.getInviterId().equals(duel.getLeaderId());
        vo.setLeaderInvite(leaderInvite);
        boolean direct = leaderInvite || duel.getJoinMode() == null
                || duel.getJoinMode() == DuelConstant.JOIN_MODE_FREE;
        vo.setJoinAction(direct ? "direct" : "apply");
        return vo;
    }

    @Override
    public InviteUseResultVO useInvite(Long userId, String code) {
        DuelInvite invite = requireByCode(code);
        Duel duel = duelMapper.selectById(invite.getDuelId());
        ThrowUtils.throwIf(duel == null, ErrorCode.NOT_FOUND_ERROR, "死斗不存在");
        ThrowUtils.throwIf(duel.getStatus() != DuelConstant.DUEL_STATUS_RECRUITING,
                ErrorCode.OPERATION_ERROR, "该死斗已开始或结束，邀请已失效");
        long joined = duelMemberMapper.selectCount(new LambdaQueryWrapper<DuelMember>()
                .eq(DuelMember::getDuelId, duel.getId())
                .eq(DuelMember::getUserId, userId)
                .ne(DuelMember::getStatus, DuelConstant.MEMBER_STATUS_QUIT));
        ThrowUtils.throwIf(joined > 0, ErrorCode.OPERATION_ERROR, "你已在该死斗中");

        boolean leaderInvite = invite.getInviterId().equals(duel.getLeaderId());
        InviteUseResultVO result = new InviteUseResultVO();
        if (leaderInvite) {
            // 组长码免审：审批制也直接入组（joinDirect 跳过模式校验，其余护栏照旧）
            result.setAction("joined");
            result.setDuel(duelService.joinDirect(userId, duel.getId()));
        } else if (duel.getJoinMode() == null || duel.getJoinMode() == DuelConstant.JOIN_MODE_FREE) {
            // 自由制成员码：直接加入
            result.setAction("joined");
            result.setDuel(duelService.join(userId, duel.getId()));
        } else {
            // 审批制 + 成员码：提交申请并留痕邀请人
            result.setAction("applied");
            result.setDuel(duelService.apply(userId, duel.getId(), invite.getInviterId()));
        }
        log.info("用户 {} 通过邀请码 {} 加入死斗 {}（{}）", userId, invite.getCode(), duel.getId(), result.getAction());
        return result;
    }

    private DuelInvite requireByCode(String code) {
        ThrowUtils.throwIf(StrUtil.isBlank(code), ErrorCode.PARAMS_ERROR, "邀请码不能为空");
        DuelInvite invite = getOne(new LambdaQueryWrapper<DuelInvite>()
                .eq(DuelInvite::getCode, code.trim().toUpperCase()));
        ThrowUtils.throwIf(invite == null, ErrorCode.NOT_FOUND_ERROR, "邀请码不存在或已失效");
        return invite;
    }

    private DuelInvite insertWithRetry(Long duelId, Long userId) {
        for (int i = 0; i < CODE_RETRY_MAX; i++) {
            DuelInvite invite = new DuelInvite();
            invite.setDuelId(duelId);
            invite.setInviterId(userId);
            invite.setCode(generateCode());
            try {
                save(invite);
                return invite;
            } catch (DuplicateKeyException e) {
                // uk_code 撞码：换一个重试；uk_duel_inviter 并发双建：按已有行返回
                DuelInvite existing = getOne(new LambdaQueryWrapper<DuelInvite>()
                        .eq(DuelInvite::getDuelId, duelId)
                        .eq(DuelInvite::getInviterId, userId));
                if (existing != null) {
                    return existing;
                }
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邀请码生成失败，请重试");
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String renderPoster(Duel duel, String code, Long inviterId) {
        try {
            User inviter = userMapper.selectById(inviterId);
            User leader = userMapper.selectById(duel.getLeaderId());
            byte[] png = PosterRenderer.renderDuelInvite(qrContentOf(code),
                    duel.getDuelName(),
                    leader == null ? "" : leader.getUserName(),
                    inviter == null ? "" : inviter.getUserName(),
                    duel.getDepositPerMember(),
                    duel.getTotalDays(),
                    duel.getMemberCount());
            return storageService.storeImage(png, "png");
        } catch (Exception e) {
            log.error("邀请海报渲染失败：duel={}, code={}", duel.getId(), code, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "海报生成失败，请重试");
        }
    }

    private String qrContentOf(String code) {
        // 上下文路径固定为 /api（两份配置一致）
        return inviteBaseUrl + "/api/duel/invite/info/" + code;
    }
}
