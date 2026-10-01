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

    /**
     * 好友数量上限（每人）
     */
    int FRIEND_MAX = 500;

    // region 猫猫战斗系统（配额制 BOSS + 随机事件）

    /**
     * 随机事件触发概率（%）
     */
    int RANDOM_EVENT_RATE = 30;

    /**
     * 随机事件四分支：物品 40 / 属性+ 25 / 属性- 25 / 饰品 10（累计阈值）
     */
    int EVENT_ITEM_MAX = 40;
    int EVENT_STAT_PLUS_MAX = 65;
    int EVENT_STAT_MINUS_MAX = 90;
    // 91~100 = 发现饰品

    /**
     * BOSS 基准血量与线性递增步长（第 n 只 = BASE + (n-1)×STEP）
     */
    int BOSS_HP_FIRST = 60;
    int BOSS_HP_STEP = 20;

    /**
     * BOSS 配额：每 7 天 1 只
     */
    int BOSS_DAYS_PER_BOSS = 7;

    /**
     * 配额打完后的喵币期奖励（个人 20 / 组队 40，每次打卡）
     */
    int COIN_PERIOD_REWARD = 20;
    int COIN_PERIOD_REWARD_TEAM = 40;

    /**
     * 全清讨伐完成礼（一次性）
     */
    int PURGE_BONUS = 100;

    /**
     * 属性降低下限（不低于初始值 10 的一半）
     */
    int STAT_MINUS_FLOOR = 5;

    // endregion

    /**
     * 黑名单数量上限（每人，防拉黑列表被当私刑工具滥用）
     */
    int BLOCK_MAX = 20;

    // endregion

    // region 猫初始属性

    int CAT_INIT_ATTACK = 10;
    int CAT_INIT_DEFENSE = 10;
    int CAT_INIT_MAX_HP = 100;

    // endregion

    int RANK_TOP_SIZE = 50;
}
