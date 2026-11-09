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

    /**
     * 收到点赞
     */
    int TYPE_LIKE = 6;

    /**
     * 死斗自动解散（进行中仅剩一人）
     */
    int TYPE_DUEL_DISBANDED = 7;

    /**
     * 收到凭证上诉（互审局上诉提交，通知组长仲裁）
     */
    int TYPE_APPEAL_SUBMITTED = 8;

    /**
     * 上诉处理结果（改判通过/维持驳回，通知上诉人）
     */
    int TYPE_APPEAL_RESOLVED = 9;
}
