package com.senze.miaokaka.constant;

import cn.hutool.core.util.RandomUtil;

/**
 * 随机名库：猫精灵名 / BOSS 名
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface NameLibraryConstant {

    /**
     * 猫精灵随机名库
     */
    String[] CAT_NAMES = {
            "喵十三", "橘座", "雪球", "煤球", "大橘", "团子", "奶盖", "麻薯",
            "布丁", "年糕", "汤圆", "芝麻", "云朵", "咖啡", "抹茶", "芋泥",
            "鱼干", "肉包", "花卷", "月饼", "咕噜", "咪酱", "馒头", "豆包"
    };

    /**
     * BOSS 随机名库（与打卡场景呼应，击杀有叙事感）
     */
    String[] BOSS_NAMES = {
            "拖延症巨兽", "焦虑魔龙", "赖床小妖", "手机幽魂", "摆烂史莱姆",
            "熬夜蝙蝠", "摸鱼章鱼", "内耗影魔", "懈怠狼人", "分心小丑",
            "懒癌魔王", "借口女妖", "躺平石像", "刷屏水蛭", "咸鱼精"
    };

    /**
     * AI 不可用时的降级鼓励语（猫口吻）
     */
    String[] FALLBACK_ENCOURAGEMENTS = {
            "喵！今天也是和铲屎官并肩作战的一天！",
            "喵呜～这份努力，本喵都记在小本本上啦！",
            "喵！再给 BOSS 一爪子，它就撑不住啦！",
            "铲屎官好样的，本喵的毛都骄傲得竖起来了！",
            "喵～坚持的每一天，都会变成我的爪印勋章！",
            "喵！本喵今晚要梦见你打倒 BOSS 的样子！",
            "喵呜～今天的你，比昨天更闪亮啦！"
    };

    static String randomCatName() {
        return CAT_NAMES[RandomUtil.randomInt(CAT_NAMES.length)];
    }

    static String randomBossName() {
        return BOSS_NAMES[RandomUtil.randomInt(BOSS_NAMES.length)];
    }

    static String randomFallbackEncouragement() {
        return FALLBACK_ENCOURAGEMENTS[RandomUtil.randomInt(FALLBACK_ENCOURAGEMENTS.length)];
    }
}
