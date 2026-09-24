package com.senze.miaokaka.constant;

/**
 * 死斗/喵币业务常量
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface DuelConstant {

    // region 玩法模式

    int DUEL_MODE_DEPOSIT = 0;
    int DUEL_MODE_TEAM = 1;

    // endregion

    // region 死斗状态

    int DUEL_STATUS_RECRUITING = 0;
    int DUEL_STATUS_RUNNING = 1;
    int DUEL_STATUS_SETTLED = 2;
    int DUEL_STATUS_DISBANDED = 3;

    // endregion

    // region 成员状态

    int MEMBER_STATUS_JOINED = 0;
    int MEMBER_STATUS_RUNNING = 1;
    int MEMBER_STATUS_SETTLED = 2;
    int MEMBER_STATUS_QUIT = 3;
    int MEMBER_STATUS_REMOVED = 4;

    // endregion

    // region 加入模式

    int JOIN_MODE_FREE = 0;
    int JOIN_MODE_APPROVAL = 1;

    // endregion

    // region 加入申请状态

    int JOIN_REQUEST_PENDING = 0;
    int JOIN_REQUEST_APPROVED = 1;
    int JOIN_REQUEST_REJECTED = 2;

    // endregion

    // region 喵币流水类型

    int COIN_TX_REGISTER_GIFT = 0;
    int COIN_TX_DEPOSIT = 1;
    int COIN_TX_REFUND = 2;
    int COIN_TX_POOL_SHARE = 3;
    int COIN_TX_ADMIN_ADJUST = 4;
    int COIN_TX_DAILY_REFUND = 5;
    int COIN_TX_DAILY_CLAWBACK = 6;

    // endregion

    // region 凭证审核状态

    int REVIEW_STATUS_PENDING = 0;
    int REVIEW_STATUS_APPROVED = 1;
    int REVIEW_STATUS_REJECTED = 2;

    // endregion

    // region 弹劾组长

    int IMPEACH_STATUS_ACTIVE = 0;
    int IMPEACH_STATUS_SUCCESS = 1;
    int IMPEACH_STATUS_FAILED = 2;

    int VOTE_MAINTAIN = 0;
    int VOTE_IMPEACH = 1;

    /**
     * 弹劾投票期限（小时）
     */
    int IMPEACH_DURATION_HOURS = 24;

    /**
     * 弹劾失败后冷却（小时）
     */
    int IMPEACH_COOLDOWN_HOURS = 24;

    // endregion

    // region 玩法参数

    int DEPOSIT_MIN = 100;
    int DEPOSIT_MAX = 5000;
    int TOTAL_DAYS_MIN = 3;
    int TOTAL_DAYS_MAX = 365;
    int MEMBER_MAX = 50;
    int MEMBER_MIN = 2;

    /**
     * 注册赠送喵币
     */
    int REGISTER_GIFT_COINS = 1000;

    // endregion
}
