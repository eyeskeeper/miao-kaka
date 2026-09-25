package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.constant.AchievementConstant;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.UserAchievementMapper;
import com.senze.miaokaka.model.entity.CatSpirit;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.entity.UserAchievement;
import com.senze.miaokaka.model.vo.AchievementVO;
import com.senze.miaokaka.service.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成就徽章服务实现。
 * 设计要点：解锁幂等（唯一键兜底，已持有不重复授予）；只增不删（历史不回收）；
 * 评估所需的计数查询在打卡主流程尾部执行，规模可控。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementServiceImpl extends ServiceImpl<UserAchievementMapper, UserAchievement>
        implements AchievementService {

    private final UserAchievementMapper userAchievementMapper;

    private final CheckInRecordMapper checkInRecordMapper;

    @Override
    public boolean award(Long userId, String code) {
        if (hasBadge(userId, code)) {
            return false;
        }
        UserAchievement achievement = new UserAchievement();
        achievement.setUserId(userId);
        achievement.setCode(code);
        achievement.setUnlockedAt(new Date());
        try {
            save(achievement);
            log.info("用户 {} 解锁徽章 {}", userId, code);
            return true;
        } catch (DuplicateKeyException e) {
            // 并发双解锁：唯一键兜底，视为已持有
            return false;
        }
    }

    @Override
    public List<String> evaluateCheckInBadges(User user, CheckInPlan plan, CatSpirit cat) {
        Long userId = user.getId();
        List<String> unlocked = new ArrayList<>();

        // 首次打卡：累计任意状态打卡记录 ≥ 1
        if (awardIf(userId, AchievementConstant.FIRST_CHECKIN, countRecords(userId, null) >= 1)) {
            unlocked.add(AchievementConstant.nameOf(AchievementConstant.FIRST_CHECKIN));
        }

        // 单计划连击里程碑（本次打卡的计划）
        int streak = plan.getCurrentStreak() == null ? 0 : plan.getCurrentStreak();
        if (awardIf(userId, AchievementConstant.STREAK_7, streak >= 7)) {
            unlocked.add(AchievementConstant.nameOf(AchievementConstant.STREAK_7));
        }
        if (awardIf(userId, AchievementConstant.STREAK_30, streak >= 30)) {
            unlocked.add(AchievementConstant.nameOf(AchievementConstant.STREAK_30));
        }
        if (awardIf(userId, AchievementConstant.STREAK_100, streak >= 100)) {
            unlocked.add(AchievementConstant.nameOf(AchievementConstant.STREAK_100));
        }

        // 全勤连击（user.current_streak）
        int fullStreak = user.getCurrentStreak() == null ? 0 : user.getCurrentStreak();
        if (awardIf(userId, AchievementConstant.FULL_WEEK, fullStreak >= 7)) {
            unlocked.add(AchievementConstant.nameOf(AchievementConstant.FULL_WEEK));
        }

        // BOSS 击败数
        int bossDefeated = cat == null || cat.getTotalBossDefeated() == null ? 0 : cat.getTotalBossDefeated();
        if (awardIf(userId, AchievementConstant.BOSS_1, bossDefeated >= 1)) {
            unlocked.add(AchievementConstant.nameOf(AchievementConstant.BOSS_1));
        }
        if (awardIf(userId, AchievementConstant.BOSS_10, bossDefeated >= 10)) {
            unlocked.add(AchievementConstant.nameOf(AchievementConstant.BOSS_10));
        }
        return unlocked;
    }

    @Override
    public boolean evaluateMakeupBadge(Long userId) {
        return countRecords(userId, CheckInConstant.RECORD_STATUS_MAKEUP) >= 1
                && award(userId, AchievementConstant.MAKEUP_FIRST);
    }

    @Override
    public List<AchievementVO> wall(Long userId) {
        Set<String> mine = list(new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getUserId, userId))
                .stream().map(UserAchievement::getCode).collect(Collectors.toSet());
        List<AchievementVO> vos = new ArrayList<>();
        for (String code : AchievementConstant.ALL_CODES) {
            AchievementVO vo = new AchievementVO();
            vo.setCode(code);
            vo.setName(AchievementConstant.nameOf(code));
            vo.setDescription(AchievementConstant.DESCRIPTIONS.get(code));
            vo.setUnlocked(mine.contains(code));
            vos.add(vo);
        }
        return vos;
    }

    private boolean awardIf(Long userId, String code, boolean condition) {
        return condition && award(userId, code);
    }

    private boolean hasBadge(Long userId, String code) {
        return userAchievementMapper.selectCount(new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getUserId, userId)
                .eq(UserAchievement::getCode, code)) > 0;
    }

    private long countRecords(Long userId, Integer status) {
        LambdaQueryWrapper<CheckInRecord> wrapper = new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getUserId, userId);
        if (status != null) {
            wrapper.eq(CheckInRecord::getStatus, status);
        }
        return checkInRecordMapper.selectCount(wrapper);
    }
}
