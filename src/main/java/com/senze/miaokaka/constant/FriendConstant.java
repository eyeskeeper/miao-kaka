package com.senze.miaokaka.constant;

/**
 * 好友/点赞常量
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface FriendConstant {

    int STATUS_PENDING = 0;
    int STATUS_AGREED = 1;

    /**
     * 点赞者每日点赞上限
     */
    int DAILY_LIKE_LIMIT = 5;

    /**
     * 好友动态回溯天数
     */
    int FEED_LOOKBACK_DAYS = 7;

    /**
     * 动态最大条数
     */
    int FEED_MAX_SIZE = 50;
}
