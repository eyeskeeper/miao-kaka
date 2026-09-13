package com.senze.miaokaka.constant;

/**
 * 拍一拍常量
 * 边界共识：模板文案入库（user.nudge_text）；"拍一拍"行为与消息只存 Redis，
 * 收件箱当日有效（北京时间 24 点过期）——不落数据库表
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface NudgeConstant {

    /**
     * 模板文案最大长度
     */
    int TEXT_MAX = 20;

    /**
     * 系统默认文案
     */
    String DEFAULT_TEXT = "戳了戳你，快去打卡！";

    /**
     * 发起方每日发送上限（不分对象，北京时间 24 点重置）
     */
    int DAILY_SEND_LIMIT = 5;

    /**
     * 收件箱上限（满则拒收）
     */
    int INBOX_CAP = 20;

    /**
     * 收件箱 key 前缀：miaokaka:nudge:inbox:{userId}
     */
    static String keyInbox(Long userId) {
        return "miaokaka:nudge:inbox:" + userId;
    }

    /**
     * 每日频控 key 前缀：miaokaka:nudge:cnt:{userId}:{yyyyMMdd}
     */
    static String keyDailyCount(Long userId, String date) {
        return "miaokaka:nudge:cnt:" + userId + ":" + date;
    }
}
