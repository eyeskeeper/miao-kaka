package com.senze.miaokaka.utils;

import com.senze.miaokaka.utils.SettlementCalculator.MemberStake;
import com.senze.miaokaka.utils.SettlementCalculator.SettlementResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 死斗结算公式单元测试
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
class SettlementCalculatorTest {

    @Test
    void 全员满勤_全额退还_无奖池() {
        SettlementResult r = SettlementCalculator.settle(7, List.of(
                new MemberStake(1L, 100, 7),
                new MemberStake(2L, 200, 7)));
        assertEquals(100, r.refunds().get(1L));
        assertEquals(200, r.refunds().get(2L));
        assertEquals(0, r.poolShares().getOrDefault(1L, 0));
        assertEquals(0, r.poolShares().getOrDefault(2L, 0));
        assertEquals(0, r.sunk());
    }

    @Test
    void 部分缺勤_没收逐笔按其余成员天数占比分配() {
        // T=7：甲满勤，乙6天，丙0天（各押100）
        // 乙退 floor(600/7)=85 没收15 → 其余仅有甲 → 甲分15
        // 丙没收100 → 其余甲7天/乙6天 → 甲 floor(700/13)=53，乙 floor(600/13)=46，沉没1
        SettlementResult r = SettlementCalculator.settle(7, List.of(
                new MemberStake(1L, 100, 7),
                new MemberStake(2L, 100, 6),
                new MemberStake(3L, 100, 0)));
        assertEquals(100, r.refunds().get(1L));
        assertEquals(85, r.refunds().get(2L));
        assertEquals(0, r.refunds().get(3L));
        assertEquals(68, r.poolShares().get(1L));
        assertEquals(46, r.poolShares().get(2L));
        assertEquals(0, r.poolShares().getOrDefault(3L, 0));
        assertEquals(1, r.sunk());
    }

    @Test
    void 全员零出勤_全部沉没() {
        SettlementResult r = SettlementCalculator.settle(7, List.of(
                new MemberStake(1L, 100, 0),
                new MemberStake(2L, 100, 0)));
        assertEquals(0, r.refunds().get(1L));
        assertEquals(0, r.refunds().get(2L));
        assertEquals(0, r.poolShares().getOrDefault(1L, 0));
        assertEquals(0, r.poolShares().getOrDefault(2L, 0));
        assertEquals(200, r.sunk());
    }

    @Test
    void 余数留在没收侧() {
        // T=7：甲4天乙3天各押100 → 甲退57没收43，乙退42没收58
        // 甲的没收43 → 其余仅乙 → 乙分43；乙的没收58 → 其余仅甲 → 甲分58；零沉没
        SettlementResult r = SettlementCalculator.settle(7, List.of(
                new MemberStake(1L, 100, 4),
                new MemberStake(2L, 100, 3)));
        assertEquals(57, r.refunds().get(1L));
        assertEquals(42, r.refunds().get(2L));
        assertEquals(58, r.poolShares().get(1L));
        assertEquals(43, r.poolShares().get(2L));
        assertEquals(0, r.sunk());
    }

    @Test
    void 防御性_天数越界被钳制() {
        SettlementResult r = SettlementCalculator.settle(7, List.of(
                new MemberStake(1L, 100, 99)));
        assertEquals(100, r.refunds().get(1L));
        assertEquals(0, r.sunk());
    }

    @Test
    void 按天数拆分_整除() {
        SettlementCalculator.SplitResult r = SettlementCalculator.splitByDays(100, List.of(
                new MemberStake(1L, 100, 7),
                new MemberStake(2L, 100, 3)));
        assertEquals(70, r.shares().get(1L));
        assertEquals(30, r.shares().get(2L));
        assertEquals(0, r.remainder());
    }

    @Test
    void 按天数拆分_余数返回调用方() {
        SettlementCalculator.SplitResult r = SettlementCalculator.splitByDays(101, List.of(
                new MemberStake(1L, 100, 7),
                new MemberStake(2L, 100, 3)));
        assertEquals(70, r.shares().get(1L));
        assertEquals(30, r.shares().get(2L));
        assertEquals(1, r.remainder());
    }

    @Test
    void 按天数拆分_全零天数全沉没() {
        SettlementCalculator.SplitResult r = SettlementCalculator.splitByDays(50, List.of(
                new MemberStake(1L, 100, 0)));
        org.junit.jupiter.api.Assertions.assertTrue(r.shares().isEmpty());
        assertEquals(50, r.remainder());
    }
}
