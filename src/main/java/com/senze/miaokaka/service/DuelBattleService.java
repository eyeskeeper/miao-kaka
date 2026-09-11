package com.senze.miaokaka.service;

import com.senze.miaokaka.model.dto.duel.ReviewRequest;
import com.senze.miaokaka.model.vo.CheckInResultVO;
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
     * 死斗打卡：上传照片凭证，记录进入待审核状态
     */
    CheckInResultVO checkIn(Long userId, Long duelId, MultipartFile image, String remark);

    /**
     * 组长待审核列表
     */
    List<ReviewItemVO> pendingList(Long userId, Long duelId);

    /**
     * 组长审核：通过则触发影子计划事件结算，驳回则记录作废
     */
    CheckInResultVO review(Long userId, Long duelId, ReviewRequest request);
}
