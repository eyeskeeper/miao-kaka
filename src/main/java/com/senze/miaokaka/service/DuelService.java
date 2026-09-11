package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.dto.duel.DuelCreateRequest;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.vo.DuelVO;

import java.util.List;

/**
 * 习惯死斗生命周期服务（创建/加入/退出/详情/开赛）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface DuelService extends IService<Duel> {

    DuelVO create(Long userId, DuelCreateRequest request);

    DuelVO join(Long userId, Long duelId);

    DuelVO quit(Long userId, Long duelId);

    DuelVO detail(Long userId, Long duelId);

    List<DuelVO> listMine(Long userId);

    /**
     * 开赛扫描：到达开始日的招募中死斗自动开始（不足 2 人则解散并全额退款）
     */
    void startDueDuels();

    /**
     * 结算扫描（委托结算服务，幂等）
     */
    void settleDueDuels();
}
