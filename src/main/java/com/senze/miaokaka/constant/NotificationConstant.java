package com.senze.miaokaka.constant;

/**
 * 用户通知常量
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface NotificationConstant {

    /**
     * 被移除出死斗
     */
    int TYPE_REMOVED_FROM_DUEL = 1;

    /**
     * 打卡提醒
     */
    int TYPE_CHECKIN_REMIND = 2;

    /**
     * 系统公告（admin 广播）
     */
    int TYPE_ANNOUNCEMENT = 3;

    /**
     * 好友申请
     */
    int TYPE_FRIEND_APPLY = 4;

    /**
     * 好友申请通过
     */
    int TYPE_FRIEND_AGREE = 5;
}
