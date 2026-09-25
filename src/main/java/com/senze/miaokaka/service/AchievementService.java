package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.entity.CatSpirit;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.AchievementVO;

import java.util.List;

/**
 * 成就徽章服务（解锁幂等：一人一徽；解锁即落库，历史不回收）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface AchievementService extends IService<com.senze.miaokaka.model.entity.UserAchievement> {

    /**
     * 授予徽章（幂等：已持有返回 false）
     *
     * @return 是否为新解锁
     */
    boolean award(Long userId, String code);

    /**
     * 打卡结算后的徽章评估：首打/单计划连击/全勤连击/BOSS 击败
     *
     * @return 本次新解锁的徽章名称列表（供打卡结果展示）
     */
    List<String> evaluateCheckInBadges(User user, CheckInPlan plan, CatSpirit cat);

    /**
     * 补卡徽章评估：首次补卡
     *
     * @return 是否新解锁「不弃不离」
     */
    boolean evaluateMakeupBadge(Long userId);

    /**
     * 徽章墙：全部徽章 + 我的解锁状态
     */
    List<AchievementVO> wall(Long userId);
}
