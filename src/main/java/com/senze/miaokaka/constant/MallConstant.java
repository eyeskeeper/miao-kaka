package com.senze.miaokaka.constant;

import java.util.List;

/**
 * 积分商城常量（商品目录 v1：补卡券 + 双倍经验卡）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface MallConstant {

    String ITEM_MAKEUP_VOUCHER = "MAKEUP_VOUCHER";
    String ITEM_DOUBLE_EXP = "DOUBLE_EXP";

    /**
     * 商品编码（保持展示顺序）
     */
    List<String> ALL_ITEMS = List.of(ITEM_MAKEUP_VOUCHER, ITEM_DOUBLE_EXP);

    static String nameOf(String code) {
        return switch (code) {
            case ITEM_MAKEUP_VOUCHER -> "补卡券";
            case ITEM_DOUBLE_EXP -> "双倍经验卡";
            default -> code;
        };
    }

    static int priceOf(String code) {
        return switch (code) {
            case ITEM_MAKEUP_VOUCHER -> 100;
            case ITEM_DOUBLE_EXP -> 80;
            default -> 0;
        };
    }

    static String descOf(String code) {
        return switch (code) {
            case ITEM_MAKEUP_VOUCHER -> "补卡时选择使用，免扣 50 积分";
            case ITEM_DOUBLE_EXP -> "打卡时自动消耗一张，本次经验 ×2";
            default -> "";
        };
    }
}
