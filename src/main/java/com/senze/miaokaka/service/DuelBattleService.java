package com.senze.miaokaka.service;

import com.senze.miaokaka.model.dto.duel.ReviewRequest;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.ReviewHistoryVO;
import com.senze.miaokaka.model.vo.ReviewItemVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 死斗打卡与凭证审核服务
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface DuelBattleService {

    /**
     * 死斗打卡：上传照片凭证，记录进入待审核状态（组内互审局按错位环算定派单审核员）
     */
    CheckInResultVO checkIn(Long userId, Long duelId, MultipartFile image, String remark);

    /**
     * 待审核凭证列表：组长审核局仅组长；组内互审局 mine=派给我的（所有成员），all=全队（仅组长、只读）
     */
    List<ReviewItemVO> pendingList(Long userId, Long duelId, String scope);

    /**
     * 组内公示的审核记录（最近 50 条，组内成员可见）
     */
    List<ReviewHistoryVO> reviewHistory(Long userId, Long duelId);

    /**
     * 审核凭证：组长审核局仅组长；组内互审局仅派单审核人。通过触发影子计划事件结算，驳回必须给理由
     */
    CheckInResultVO review(Long userId, Long duelId, ReviewRequest request);

    /**
     * 超时兜底：进行中局提交超 24 小时仍待审的凭证自动通过，返回处理条数（定时任务每小时调用）
     */
    int autoApproveOverdue();

    /**
     * 组内互审局：被移除成员名下在途待审凭证按剩余在册成员重算派单（提交日口径），返回重派条数。
     * 在外层事务内调用（不另开事务）。
     */
    int reassignPending(Duel duel, Long removedUserId);

    /**
     * 一局全部待审凭证直接自动通过（解散/仅剩一人场景），返回处理条数。在外层事务内调用。
     */
    int approveAllPending(Long duelId, String remark);

    /**
     * 成员被移除时收口其本人的在途待审凭证：凭证置通过、记录置正常（不走猫事件结算——
     * 其影子计划即将下架无结算宿主）。在外层事务内调用，返回处理条数。
     */
    int closePendingOfUser(Long duelId, Long userId, String remark);
}
