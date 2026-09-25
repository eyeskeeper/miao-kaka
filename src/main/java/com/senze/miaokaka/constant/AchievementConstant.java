package com.senze.miaokaka.constant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成就徽章常量（code → 名称/解锁条件文案）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface AchievementConstant {

    String FIRST_CHECKIN = "FIRST_CHECKIN";
    String STREAK_7 = "STREAK_7";
    String STREAK_30 = "STREAK_30";
    String STREAK_100 = "STREAK_100";
    String FULL_WEEK = "FULL_WEEK";
    String BOSS_1 = "BOSS_1";
    String BOSS_10 = "BOSS_10";
    String MAKEUP_FIRST = "MAKEUP_FIRST";

    /**
     * 徽章名称（code → 名称）
     */
    Map<String, String> NAMES = Map.of(
            FIRST_CHECKIN, "初来乍到",
            STREAK_7, "七日之约",
            STREAK_30, "月度坚持",
            STREAK_100, "百日长征",
            FULL_WEEK, "全勤之星",
            BOSS_1, "猫武士",
            BOSS_10, "屠龙者",
            MAKEUP_FIRST, "不弃不离"
    );

    /**
     * 徽章解锁条件文案（code → 文案，徽章墙灰显用）
     */
    Map<String, String> DESCRIPTIONS = Map.of(
            FIRST_CHECKIN, "完成第一次打卡",
            STREAK_7, "单个计划连续打卡 7 天",
            STREAK_30, "单个计划连续打卡 30 天",
            STREAK_100, "单个计划连续打卡 100 天",
            FULL_WEEK, "全勤连击达到 7 天",
            BOSS_1, "击败第 1 只 BOSS",
            BOSS_10, "累计击败 10 只 BOSS",
            MAKEUP_FIRST, "完成第一次补卡"
    );

    /**
     * 全部徽章编码（保持展示顺序）
     */
    List<String> ALL_CODES = List.of(
            FIRST_CHECKIN, STREAK_7, STREAK_30, STREAK_100,
            FULL_WEEK, BOSS_1, BOSS_10, MAKEUP_FIRST
    );

    static String nameOf(String code) {
        return NAMES.getOrDefault(code, code);
    }
}
