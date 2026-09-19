package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.dto.duel.DuelCreateRequest;
import com.senze.miaokaka.model.dto.duel.JoinApplicationReviewRequest;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.vo.DuelVO;
import com.senze.miaokaka.model.vo.JoinRequestVO;

import java.util.List;

/**
 * 习惯死斗生命周期服务（创建/加入/申请审批/退出/移除/让渡/详情/开赛）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface DuelService extends IService<Duel> {

    DuelVO create(Long userId, DuelCreateRequest request);

    DuelVO join(Long userId, Long duelId);

    /**
     * 直接入组（邀请免审路径）：跳过审批模式校验（组长邀请码用），其余护栏（招募中/成员查重/扣押金）与 join 一致
     */
    DuelVO joinDirect(Long userId, Long duelId);

    /**
     * 申请加入（审批模式死斗；不扣押金，组长批准时才扣）
     */
    DuelVO apply(Long userId, Long duelId);

    /**
     * 申请加入（邀请码路径）：落库时留痕邀请人
     */
    DuelVO apply(Long userId, Long duelId, Long inviterId);

    /**
     * 待审加入申请列表（仅组长，审批模式）
     */
    List<JoinRequestVO> applications(Long userId, Long duelId);

    /**
     * 审批加入申请：通过时扣押金入组（余额不足自动拒绝并留痕）
     *
     * @return 审批结果说明
     */
    String reviewApplication(Long userId, Long duelId, JoinApplicationReviewRequest request);

    /**
     * 组长移除成员：招募中=全额退款；进行中=即时结算（退剩余天数份额，缺勤份额入罚没池）
     */
    DuelVO removeMember(Long userId, Long duelId, Long targetUserId);

    /**
     * 让渡组长：目标须为正式成员；即时生效，原组长降为普通成员
     */
    DuelVO transfer(Long userId, Long duelId, Long targetUserId);

    DuelVO quit(Long userId, Long duelId);

    DuelVO detail(Long userId, Long duelId);

    List<DuelVO> listMine(Long userId);

    /**
     * 死斗大厅：全量招募中 + 进行中的死斗分页（招募中优先、最新在前，轻量脱敏不带成员明细）；
     * myRelation 标识观看者与各局的关系（leader/member/applicant/null）
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.senze.miaokaka.model.vo.DuelHallVO> hall(
            long pageNum, long pageSize, Long viewerId);

    /**
     * 开赛扫描：到达开始日的招募中死斗自动开始（不足 2 人则解散并全额退款；待审申请自动拒绝）
     */
    void startDueDuels();

    /**
     * 结算扫描（委托结算服务，幂等）
     */
    void settleDueDuels();
}
