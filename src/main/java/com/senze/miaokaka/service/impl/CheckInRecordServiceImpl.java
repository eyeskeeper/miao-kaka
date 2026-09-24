package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.constant.GameConstants;
import com.senze.miaokaka.constant.NameLibraryConstant;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.CatSpiritMapper;
import com.senze.miaokaka.mapper.CheckInPlanMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.plan.TaskToggleRequest;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.CatSpirit;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.CheckInCalendarVO;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.MakeupResultVO;
import com.senze.miaokaka.model.vo.TaskToggleVO;
import com.senze.miaokaka.service.AiAssistantService;
import com.senze.miaokaka.service.CacheService;
import com.senze.miaokaka.service.CatSpiritService;
import com.senze.miaokaka.service.CheckInPlanService;
import com.senze.miaokaka.service.CheckInRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 打卡记录服务实现：事件引擎 + 补卡 + 死斗审核通过结算
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInRecordServiceImpl extends ServiceImpl<CheckInRecordMapper, CheckInRecord>
        implements CheckInRecordService {

    private final CheckInPlanService checkInPlanService;

    private final CatSpiritService catSpiritService;

    private final CatSpiritMapper catSpiritMapper;

    private final CheckInPlanMapper checkInPlanMapper;

    private final UserMapper userMapper;

    private final AiAssistantService aiAssistantService;

    private final TransactionTemplate transactionTemplate;

    private final CacheService cacheService;

    // region 打卡（事件引擎）

    @Override
    public CheckInResultVO checkIn(Long userId, Long planId, String remark) {
        // 事务内完成数据落库与结算，AI 调用放在事务外，避免长事务占住连接
        CheckInResultVO result = transactionTemplate.execute(status -> doCheckInInTx(userId, planId, remark));
        result.setEncouragement(aiAssistantService.generateEncouragement(result.getCatName(), result.getEventDesc()));
        return result;
    }

    private CheckInResultVO doCheckInInTx(Long userId, Long planId, String remark) {
        User user = userMapper.selectById(userId);
        CheckInPlan plan = checkInPlanMapper.selectById(planId);
        ThrowUtils.throwIf(plan == null, ErrorCode.NOT_FOUND_ERROR, "计划不存在");
        ThrowUtils.throwIf(!plan.getUserId().equals(userId), ErrorCode.FORBIDDEN_ERROR, "无权操作该计划");
        // 影子计划通道封闭：死斗打卡必须走死斗入口（照片凭证 + 审核）
        ThrowUtils.throwIf(plan.getPlanSource() != null && plan.getPlanSource() == CheckInConstant.PLAN_SOURCE_DUEL,
                ErrorCode.OPERATION_ERROR, "死斗计划请通过死斗打卡入口提交凭证");
        ThrowUtils.throwIf(plan.getStatus() != CheckInConstant.PLAN_STATUS_ACTIVE,
                ErrorCode.OPERATION_ERROR, "计划不在进行中，请先恢复计划");
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        CatSpirit cat = catSpiritService.getByPlanId(planId);
        ThrowUtils.throwIf(cat == null, ErrorCode.SYSTEM_ERROR, "猫精灵数据缺失");

        CheckInResultVO vo = new CheckInResultVO();
        vo.setCheckInDate(today);
        vo.setRemark(StrUtil.blankToDefault(remark, null));
        vo.setCatName(cat.getCatName());
        vo.setLevelUp(false);
        vo.setBossDefeated(false);

        // 1. 打卡记录（唯一键兜底并发双击）
        CheckInRecord record = new CheckInRecord();
        record.setUserId(userId);
        record.setPlanId(planId);
        record.setCheckInDate(today);
        record.setCheckInTime(new Date());
        record.setStatus(CheckInConstant.RECORD_STATUS_NORMAL);
        record.setRemark(StrUtil.blankToDefault(remark, null));
        try {
            save(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今天已经打过卡啦，明天再来");
        }

        // 2. 计划连击：昨天有记录则 +1，否则从 1 重算
        boolean yesterdayChecked = existsRecord(userId, planId, today.minusDays(1));
        int newStreak = yesterdayChecked ? plan.getCurrentStreak() + 1 : 1;
        plan.setCurrentStreak(newStreak);
        plan.setMaxStreak(Math.max(plan.getMaxStreak() == null ? 0 : plan.getMaxStreak(), newStreak));
        vo.setCurrentStreak(plan.getCurrentStreak());
        vo.setMaxStreak(plan.getMaxStreak());

        applyGrowthAndPoints(user, plan, cat, vo);
        return vo;
    }

    @Override
    public CheckInResultVO settleApprovedCheckIn(Long userId, Long planId, Long recordId) {
        CheckInResultVO result = transactionTemplate.execute(status -> doSettleApprovedInTx(userId, planId, recordId));
        result.setEncouragement(aiAssistantService.generateEncouragement(result.getCatName(), result.getEventDesc()));
        return result;
    }

    // region 每日任务勾选（部分打卡）

    @Override
    public TaskToggleVO toggleTask(Long userId, Long planId, TaskToggleRequest request) {
        TaskToggleVO vo = transactionTemplate.execute(status -> doToggleTaskInTx(userId, planId, request));
        // 自动打卡的猫口吻鼓励语在事务提交后生成（与普通打卡同一模式）
        if (Boolean.TRUE.equals(vo.getAutoChecked())) {
            vo.getCheckInResult().setEncouragement(aiAssistantService.generateEncouragement(
                    vo.getCheckInResult().getCatName(), vo.getCheckInResult().getEventDesc()));
        }
        return vo;
    }

    private TaskToggleVO doToggleTaskInTx(Long userId, Long planId, TaskToggleRequest request) {
        CheckInPlan plan = checkInPlanMapper.selectById(planId);
        ThrowUtils.throwIf(plan == null, ErrorCode.NOT_FOUND_ERROR, "计划不存在");
        ThrowUtils.throwIf(!plan.getUserId().equals(userId), ErrorCode.FORBIDDEN_ERROR, "无权操作该计划");
        ThrowUtils.throwIf(plan.getStatus() != CheckInConstant.PLAN_STATUS_ACTIVE,
                ErrorCode.OPERATION_ERROR, "计划不在进行中");

        List<String> tasks = parseDailyTasks(plan.getDailyTasks());
        ThrowUtils.throwIf(tasks.isEmpty(), ErrorCode.OPERATION_ERROR, "该计划未配置每日任务，直接打卡即可");
        int idx = request.getTaskIndex();
        ThrowUtils.throwIf(idx >= tasks.size(), ErrorCode.PARAMS_ERROR, "任务下标超出范围（共 " + tasks.size() + " 项）");

        // 当日已有任意状态记录（正常/待审核/异常）即冻结：数据已随打卡落定
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        ThrowUtils.throwIf(existsRecord(userId, planId, today),
                ErrorCode.OPERATION_ERROR, "今日已打卡，任务勾选已冻结");

        // 位图翻转（幂等）：补齐长度 → 翻转指定项 → 统计完成数
        boolean done = Boolean.TRUE.equals(request.getDone());
        char[] progress = padProgress(plan.getTaskProgress(), tasks.size());
        progress[idx] = done ? '1' : '0';
        String progressStr = new String(progress);
        int completed = countDone(progressStr);
        plan.setTaskProgress(progressStr);
        plan.setCompletedTasks(completed);
        checkInPlanMapper.updateById(plan);

        TaskToggleVO vo = new TaskToggleVO();
        vo.setPlanId(planId);
        vo.setTaskIndex(idx);
        vo.setDone(done);
        vo.setTaskProgress(progressStr);
        vo.setCompletedTasks(completed);
        vo.setTotalTasks(tasks.size());
        boolean allDone = completed >= tasks.size();
        vo.setAllDone(allDone);
        vo.setAutoChecked(false);

        if (allDone) {
            if (plan.getPlanSource() != null && plan.getPlanSource() == CheckInConstant.PLAN_SOURCE_DUEL) {
                // 影子计划通道封闭：完成勾选后仍需照片凭证，不自动打卡
                vo.setMessage("任务全部完成！请到死斗入口上传照片凭证完成打卡");
            } else {
                // 勾满即打卡：复用事件引擎，奖励即刻落地
                vo.setCheckInResult(doCheckInInTx(userId, planId, null));
                vo.setAutoChecked(true);
            }
        }
        return vo;
    }

    private List<String> parseDailyTasks(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            List<String> tasks = cn.hutool.json.JSONUtil.toList(json, String.class);
            return tasks == null ? List.of() : tasks.stream().filter(StrUtil::isNotBlank).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private char[] padProgress(String progress, int size) {
        char[] chars = new char[size];
        java.util.Arrays.fill(chars, '0');
        if (StrUtil.isNotBlank(progress)) {
            for (int i = 0; i < Math.min(progress.length(), size); i++) {
                chars[i] = progress.charAt(i) == '1' ? '1' : '0';
            }
        }
        return chars;
    }

    private int countDone(String progress) {
        int count = 0;
        for (char c : progress.toCharArray()) {
            if (c == '1') {
                count++;
            }
        }
        return count;
    }

    // endregion

    /**
     * 死斗凭证审核通过的结算：记录置为正常、回溯重算连击、事件在通过那一刻才触发
     */
    private CheckInResultVO doSettleApprovedInTx(Long userId, Long planId, Long recordId) {
        User user = userMapper.selectById(userId);
        CheckInPlan plan = checkInPlanMapper.selectById(planId);
        ThrowUtils.throwIf(plan == null || !plan.getUserId().equals(userId),
                ErrorCode.NOT_FOUND_ERROR, "影子计划不存在");
        CheckInRecord record = getById(recordId);
        ThrowUtils.throwIf(record == null
                        || !record.getUserId().equals(userId)
                        || !record.getPlanId().equals(planId),
                ErrorCode.NOT_FOUND_ERROR, "打卡记录不存在");
        ThrowUtils.throwIf(record.getStatus() != CheckInConstant.RECORD_STATUS_PENDING,
                ErrorCode.OPERATION_ERROR, "该记录不在待审核状态");
        CatSpirit cat = catSpiritService.getByPlanId(planId);
        ThrowUtils.throwIf(cat == null, ErrorCode.SYSTEM_ERROR, "猫精灵数据缺失");

        record.setStatus(CheckInConstant.RECORD_STATUS_NORMAL);
        updateById(record);

        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        recomputePlanStreak(plan, userId, today);

        CheckInResultVO vo = new CheckInResultVO();
        vo.setCheckInDate(record.getCheckInDate());
        vo.setRemark(record.getRemark());
        vo.setCatName(cat.getCatName());
        vo.setLevelUp(false);
        vo.setBossDefeated(false);
        vo.setCurrentStreak(plan.getCurrentStreak());
        vo.setMaxStreak(plan.getMaxStreak());
        applyGrowthAndPoints(user, plan, cat, vo);
        return vo;
    }

    /**
     * 事件 + 成长 + 积分的共用结算（普通打卡与死斗审核通过共用）。
     * 调用前需已设置 plan.currentStreak / vo 的连击与猫名等基础字段。
     */
    private void applyGrowthAndPoints(User user, CheckInPlan plan, CatSpirit cat, CheckInResultVO vo) {
        // 1. 事件：1~70 攻击 / 71~95 属性提升 / 96~100 暴击
        int expGained = GameConstants.EXP_PER_CHECK_IN;
        int pointsEarned = GameConstants.POINTS_PER_CHECK_IN;
        int roll = ThreadLocalRandom.current().nextInt(1, 101);
        if (roll <= GameConstants.EVENT_ATTACK_MAX || roll > GameConstants.EVENT_STAT_MAX) {
            EventReward reward = resolveAttack(cat, plan.getCurrentStreak(),
                    roll > GameConstants.EVENT_STAT_MAX, vo);
            expGained += reward.expBonus();
            pointsEarned += reward.pointsBonus();
        } else {
            resolveStatBoost(cat, vo);
        }

        // 2. 经验结算与升级（升级三维各 +10%，并回满血）
        int exp = cat.getExperience() + expGained;
        int level = cat.getLevel();
        boolean levelUp = false;
        while (exp >= CatSpiritServiceImpl.expToNextLevel(level)) {
            exp -= CatSpiritServiceImpl.expToNextLevel(level);
            level++;
            levelUp = true;
            cat.setAttack(grow(cat.getAttack()));
            cat.setDefense(grow(cat.getDefense()));
            cat.setMaxHp(grow(cat.getMaxHp()));
            cat.setCurrentHp(cat.getMaxHp());
        }
        cat.setExperience(exp);
        cat.setLevel(level);
        vo.setLevel(level);
        vo.setLevelUp(levelUp);
        catSpiritMapper.updateById(cat);

        // 3. 积分：打卡基础分 + 连击每满 7 天里程碑奖励
        if (plan.getCurrentStreak() % GameConstants.POINTS_STREAK_MILESTONE == 0) {
            pointsEarned += GameConstants.POINTS_STREAK_MILESTONE_BONUS;
        }

        // 4. 全勤连击：本次若恰好补齐"全部进行中计划"，按昨日是否全勤累计/重置
        refreshFullAttendanceStreak(user, LocalDate.now(CheckInConstant.BIZ_ZONE));

        // 5. 积分落库
        User freshUser = userMapper.selectById(user.getId());
        freshUser.setTotalPoints(freshUser.getTotalPoints() + pointsEarned);
        userMapper.updateById(freshUser);
        // 用户行已变（积分/全勤连击）：逐出登录态与排行榜缓存
        cacheService.evict(CacheService.keyUser(user.getId()), CacheService.KEY_RANK_STREAK);
        vo.setExpGained(expGained);
        vo.setPointsEarned(pointsEarned);
        vo.setTotalPoints(freshUser.getTotalPoints());

        // 6. 打卡落定即开启新一天：清空任务位图。
        // 位图若跨天残留，次日任意一次勾选都会立即重新满足 allDone 并自动打卡（一次点击完成全天）
        plan.setTaskProgress("");
        plan.setCompletedTasks(0);

        checkInPlanMapper.updateById(plan);
    }

    /**
     * 攻击/暴击事件；若击败 BOSS 则结算奖励并刷新下一只满血 BOSS
     */
    private EventReward resolveAttack(CatSpirit cat, int streak, boolean isCrit, CheckInResultVO vo) {
        double factor = GameConstants.DAMAGE_MIN_FACTOR
                + ThreadLocalRandom.current().nextDouble(GameConstants.DAMAGE_MAX_FACTOR - GameConstants.DAMAGE_MIN_FACTOR);
        double damage = cat.getAttack() * (1 + streak * GameConstants.STREAK_DAMAGE_BONUS_PER_DAY) * factor;
        if (isCrit) {
            damage *= GameConstants.CRIT_DAMAGE_MULTIPLIER;
        }
        int realDamage = Math.max(1, (int) Math.round(damage));
        int hpBefore = cat.getBossHp();
        int hpAfter = Math.max(0, hpBefore - realDamage);
        cat.setBossHp(hpAfter);

        vo.setEventType(isCrit ? CheckInConstant.EVENT_TYPE_CRIT : CheckInConstant.EVENT_TYPE_ATTACK);
        vo.setBossName(cat.getBossName());
        vo.setBossHpBefore(hpBefore);
        vo.setBossHpAfter(hpAfter);
        vo.setDamage(realDamage);
        vo.setEventDesc(isCrit
                ? String.format("会心一击！%s 扑向 %s，造成 %d 点伤害！", cat.getCatName(), cat.getBossName(), realDamage)
                : String.format("%s 扑向 %s，造成 %d 点伤害！", cat.getCatName(), cat.getBossName(), realDamage));

        if (hpAfter > 0) {
            return new EventReward(0, 0);
        }
        int expBonus = GameConstants.EXP_PER_BOSS_KILL_BASE * cat.getBossLevel();
        int pointsBonus = GameConstants.POINTS_PER_BOSS_KILL_BASE * cat.getBossLevel();
        cat.setTotalBossDefeated(cat.getTotalBossDefeated() + 1);
        int newBossLevel = cat.getBossLevel() + 1;
        int newBossHp = CatSpiritServiceImpl.bossMaxHp(newBossLevel);
        String oldBossName = cat.getBossName();
        cat.setBossLevel(newBossLevel);
        cat.setBossMaxHp(newBossHp);
        cat.setBossHp(newBossHp);
        cat.setBossName(NameLibraryConstant.randomBossName());
        vo.setBossDefeated(true);
        vo.setNewBossLevel(newBossLevel);
        vo.setNewBossName(cat.getBossName());
        vo.setNewBossMaxHp(newBossHp);
        vo.setEventDesc(vo.getEventDesc() + String.format(" %s 倒下了！下一只 BOSS %s（Lv.%d）登场！",
                oldBossName, cat.getBossName(), newBossLevel));
        return new EventReward(expBonus, pointsBonus);
    }

    private void resolveStatBoost(CatSpirit cat, CheckInResultVO vo) {
        int gain = ThreadLocalRandom.current().nextInt(
                GameConstants.STAT_BOOST_MIN, GameConstants.STAT_BOOST_MAX + 1);
        int pick = ThreadLocalRandom.current().nextInt(3);
        switch (pick) {
            case 0 -> {
                cat.setAttack(cat.getAttack() + gain);
                vo.setStatName("attack");
                vo.setEventDesc(String.format("%s 磨了磨爪子，攻击力提升 %d 点！", cat.getCatName(), gain));
            }
            case 1 -> {
                cat.setDefense(cat.getDefense() + gain);
                vo.setStatName("defense");
                vo.setEventDesc(String.format("%s 打了个滚，皮毛更厚实了，防御力提升 %d 点！", cat.getCatName(), gain));
            }
            default -> {
                cat.setMaxHp(cat.getMaxHp() + gain);
                cat.setCurrentHp(cat.getCurrentHp() + gain);
                vo.setStatName("hp");
                vo.setEventDesc(String.format("%s 找到了猫薄荷，精力充沛，生命上限提升 %d 点！", cat.getCatName(), gain));
            }
        }
        vo.setEventType(CheckInConstant.EVENT_TYPE_STAT);
        vo.setStatGain(gain);
    }

    /**
     * 事件奖励（BOSS 击败加成）
     */
    private record EventReward(int expBonus, int pointsBonus) {
    }

    // endregion

    // region 补卡

    @Override
    public MakeupResultVO makeup(Long userId, Long planId, String date) {
        LocalDate makeupDate;
        try {
            makeupDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日期格式应为 yyyy-MM-dd");
        }
        return transactionTemplate.execute(status -> doMakeupInTx(userId, planId, makeupDate));
    }

    private MakeupResultVO doMakeupInTx(Long userId, Long planId, LocalDate makeupDate) {
        User user = userMapper.selectById(userId);
        CheckInPlan plan = checkInPlanService.getOwnedPlan(userId, planId);
        // 影子计划通道封闭：死斗缺卡直接按比例没收，不允许补卡
        ThrowUtils.throwIf(plan.getPlanSource() != null && plan.getPlanSource() == CheckInConstant.PLAN_SOURCE_DUEL,
                ErrorCode.OPERATION_ERROR, "死斗计划不支持补卡，缺卡将按比例没收押金");

        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        ThrowUtils.throwIf(!makeupDate.isBefore(today), ErrorCode.PARAMS_ERROR, "只能补今天之前的卡");
        ThrowUtils.throwIf(makeupDate.isBefore(today.minusDays(GameConstants.MAKEUP_MAX_LOOKBACK_DAYS)),
                ErrorCode.PARAMS_ERROR, "最多只能补最近 " + GameConstants.MAKEUP_MAX_LOOKBACK_DAYS + " 天内的卡");
        LocalDate planCreatedDate = plan.getCreateTime().toInstant().atZone(CheckInConstant.BIZ_ZONE).toLocalDate();
        ThrowUtils.throwIf(makeupDate.isBefore(planCreatedDate), ErrorCode.PARAMS_ERROR, "补卡日期不能早于计划创建日期");
        ThrowUtils.throwIf(existsRecord(userId, planId, makeupDate), ErrorCode.PARAMS_ERROR, "该日期已有打卡记录");

        // 自然月限额（用户维度）
        long used = count(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getUserId, userId)
                .eq(CheckInRecord::getStatus, CheckInConstant.RECORD_STATUS_MAKEUP)
                .ge(CheckInRecord::getCheckInDate, makeupDate.withDayOfMonth(1))
                .le(CheckInRecord::getCheckInDate, makeupDate.withDayOfMonth(makeupDate.lengthOfMonth())));
        ThrowUtils.throwIf(used >= GameConstants.MAKEUP_MONTHLY_LIMIT,
                ErrorCode.OPERATION_ERROR, "本月补卡次数已用完（每月 " + GameConstants.MAKEUP_MONTHLY_LIMIT + " 次）");
        ThrowUtils.throwIf(user.getTotalPoints() < GameConstants.MAKEUP_COST,
                ErrorCode.OPERATION_ERROR, "积分不足，补卡需要 " + GameConstants.MAKEUP_COST + " 积分");

        // 扣积分 + 落补卡记录
        user.setTotalPoints(user.getTotalPoints() - GameConstants.MAKEUP_COST);
        userMapper.updateById(user);
        cacheService.evict(CacheService.keyUser(userId));
        CheckInRecord record = new CheckInRecord();
        record.setUserId(userId);
        record.setPlanId(planId);
        record.setCheckInDate(makeupDate);
        record.setCheckInTime(new Date());
        record.setStatus(CheckInConstant.RECORD_STATUS_MAKEUP);
        save(record);

        // 从最近打卡日回溯重算计划连击（补上缺口可恢复断链）
        recomputePlanStreak(plan, userId, today);
        checkInPlanMapper.updateById(plan);

        MakeupResultVO vo = new MakeupResultVO();
        vo.setCheckInDate(makeupDate);
        vo.setPointsCost(GameConstants.MAKEUP_COST);
        vo.setTotalPoints(user.getTotalPoints());
        vo.setCurrentStreak(plan.getCurrentStreak());
        vo.setMaxStreak(plan.getMaxStreak());
        return vo;
    }

    /**
     * 连击 = 从指定锚点日（当天有记录则当天，否则最近一个打卡日）往前的连续天数
     */
    private void recomputePlanStreak(CheckInPlan plan, Long userId, LocalDate today) {
        Set<LocalDate> dates = list(new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getPlanId, plan.getId())
                        .eq(CheckInRecord::getStatus, CheckInConstant.RECORD_STATUS_NORMAL)
                        .le(CheckInRecord::getCheckInDate, today))
                .stream()
                .map(CheckInRecord::getCheckInDate)
                .collect(Collectors.toSet());
        int streak = 0;
        LocalDate cursor = dates.contains(today) ? today
                : dates.stream().filter(d -> d.isBefore(today)).max(LocalDate::compareTo).orElse(null);
        while (cursor != null && dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        plan.setCurrentStreak(streak);
        plan.setMaxStreak(Math.max(plan.getMaxStreak() == null ? 0 : plan.getMaxStreak(), streak));
    }

    // endregion

    // region 日历查询

    @Override
    public CheckInCalendarVO calendar(Long userId, Long planId, String month) {
        checkInPlanService.getOwnedPlan(userId, planId);
        YearMonth yearMonth;
        if (StrUtil.isBlank(month)) {
            yearMonth = YearMonth.from(LocalDate.now(CheckInConstant.BIZ_ZONE));
        } else {
            try {
                yearMonth = YearMonth.parse(month);
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "月份格式应为 yyyy-MM");
            }
        }
        List<CheckInRecord> records = list(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getUserId, userId)
                .eq(CheckInRecord::getPlanId, planId)
                .ge(CheckInRecord::getCheckInDate, yearMonth.atDay(1))
                .le(CheckInRecord::getCheckInDate, yearMonth.atEndOfMonth()));
        CheckInCalendarVO vo = new CheckInCalendarVO();
        vo.setPlanId(planId);
        vo.setMonth(yearMonth.toString());
        vo.setDays(records.stream().map(r -> {
            CheckInCalendarVO.DayRecord day = new CheckInCalendarVO.DayRecord();
            day.setDate(r.getCheckInDate());
            day.setStatus(r.getStatus());
            day.setRemark(r.getRemark());
            return day;
        }).toList());
        return vo;
    }

    // endregion

    // region 私有工具

    private boolean existsRecord(Long userId, Long planId, LocalDate date) {
        return baseMapper.exists(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getUserId, userId)
                .eq(CheckInRecord::getPlanId, planId)
                .eq(CheckInRecord::getCheckInDate, date));
    }

    /**
     * 全勤连击：当天全部进行中计划都有记录时，按昨天是否全勤来 +1 或重置为 1
     * （以当前活跃计划数为基准的近似实现，暂停/新建计划的跨日边界不回溯修正）
     */
    private void refreshFullAttendanceStreak(User user, LocalDate today) {
        long activePlanCount = checkInPlanMapper.selectCount(new LambdaQueryWrapper<CheckInPlan>()
                .eq(CheckInPlan::getUserId, user.getId())
                .eq(CheckInPlan::getStatus, CheckInConstant.PLAN_STATUS_ACTIVE));
        if (activePlanCount <= 0) {
            return;
        }
        long todayRecordCount = count(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getUserId, user.getId())
                .eq(CheckInRecord::getCheckInDate, today));
        if (todayRecordCount < activePlanCount) {
            return;
        }
        long yesterdayRecordCount = count(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getUserId, user.getId())
                .eq(CheckInRecord::getCheckInDate, today.minusDays(1)));
        int fullStreak = yesterdayRecordCount >= activePlanCount
                ? (user.getCurrentStreak() == null ? 0 : user.getCurrentStreak()) + 1
                : 1;
        user.setCurrentStreak(fullStreak);
        userMapper.updateById(user);
    }

    private int grow(int value) {
        return Math.max(1, (int) Math.round(value * (1 + GameConstants.LEVEL_UP_GROWTH)));
    }

    // endregion
}
