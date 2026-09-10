package com.senze.miaokaka.constant;

/**
 * 游戏数值常量（打卡 → 事件 → 猫成长/BOSS战/积分）
 * 数值集中在此，便于后续调平衡
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface GameConstants {

    // region 事件概率（百分比，命中区间：1~70攻击 / 71~95属性提升 / 96~100暴击）

    int EVENT_ATTACK_MAX = 70;
    int EVENT_STAT_MAX = 95;

    // endregion

    // region 伤害公式：攻击力 × (1 + 计划连击 × 每日加成) × 随机浮动；暴击再乘暴击倍率

    double STREAK_DAMAGE_BONUS_PER_DAY = 0.02;
    double DAMAGE_MIN_FACTOR = 0.8;
    double DAMAGE_MAX_FACTOR = 1.2;
    double CRIT_DAMAGE_MULTIPLIER = 2.0;

    // endregion

    // region BOSS：满血 = 基数 × BOSS等级^指数；血量不自动回复

    double BOSS_HP_BASE = 100.0;
    double BOSS_HP_EXPONENT = 1.3;

    // endregion

    // region 属性提升事件：攻/防/HP 随机一项提升

    int STAT_BOOST_MIN = 2;
    int STAT_BOOST_MAX = 5;

    // endregion

    // region 经验与升级：升级需求 = 基数 × 等级^指数；升级时三维各成长

    int EXP_PER_CHECK_IN = 10;
    int EXP_PER_BOSS_KILL_BASE = 20;
    double LEVEL_UP_EXP_BASE = 50.0;
    double LEVEL_UP_EXP_EXPONENT = 1.5;
    double LEVEL_UP_GROWTH = 0.1;

    // endregion

    // region 积分

    int POINTS_PER_CHECK_IN = 10;
    int POINTS_STREAK_MILESTONE = 7;
    int POINTS_STREAK_MILESTONE_BONUS = 20;
    int POINTS_PER_BOSS_KILL_BASE = 10;
    int MAKEUP_COST = 50;

    // endregion

    // region 补卡

    int MAKEUP_MONTHLY_LIMIT = 2;
    int MAKEUP_MAX_LOOKBACK_DAYS = 30;

    // endregion

    // region 猫初始属性

    int CAT_INIT_ATTACK = 10;
    int CAT_INIT_DEFENSE = 10;
    int CAT_INIT_MAX_HP = 100;

    // endregion

    int RANK_TOP_SIZE = 50;
}
