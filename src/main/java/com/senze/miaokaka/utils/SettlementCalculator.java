package com.senze.miaokaka.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 死斗结算纯函数：返还 = 押金 × 本人确认天数 / T（向下取整）；
 * 被没收部分逐笔按其余成员确认天数占比分配，无人可分则沉没。
 * 无任何 IO，便于单元测试。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    /**
     * 成员押金与确认天数
     */
    public record MemberStake(Long userId, int deposit, int days) {
    }

    /**
     * 结算结果：各成员返还额与奖池分得额（不含本金以外的任何调整）
     */
    public record SettlementResult(Map<Long, Integer> refunds, Map<Long, Integer> poolShares, int sunk) {
    }

    /**
     * 按天数占比拆分一笔金额（移除罚没池/奖池共用）：floor 分配，余数返回给调用方沉没
     */
    public static SplitResult splitByDays(int amount, List<MemberStake> members) {
        Map<Long, Integer> shares = new HashMap<>();
        long totalDays = members == null ? 0
                : members.stream().mapToLong(m -> Math.max(m.days(), 0)).sum();
        int allocated = 0;
        if (amount > 0 && totalDays > 0) {
            for (MemberStake m : members) {
                int days = Math.max(m.days(), 0);
                if (days == 0) {
                    continue;
                }
                int share = amount * days / (int) totalDays;
                if (share > 0) {
                    shares.merge(m.userId(), share, Integer::sum);
                    allocated += share;
                }
            }
        }
        return new SplitResult(shares, amount - allocated);
    }

    /**
     * 拆分结果：分得明细 + 未能分配（沉没）的余数
     */
    public record SplitResult(Map<Long, Integer> shares, int remainder) {
    }

    /**
     * @param totalDays 挑战总天数 T（>0）
     * @param members   成员押金与确认天数（days 允许 > T 的防御性钳制在此处理）
     */
    public static SettlementResult settle(int totalDays, List<MemberStake> members) {
        Map<Long, Integer> refunds = new HashMap<>();
        Map<Long, Integer> shares = new HashMap<>();
        int[] sunk = {0};
        if (members == null || members.isEmpty() || totalDays <= 0) {
            return new SettlementResult(refunds, shares, 0);
        }
        // 1. 自退 + 计算没收
        Map<Long, Integer> forfeits = new HashMap<>();
        for (MemberStake m : members) {
            int days = Math.min(Math.max(m.days(), 0), totalDays);
            int refund = Math.min(m.deposit(), m.deposit() * days / totalDays);
            refunds.put(m.userId(), refund);
            forfeits.put(m.userId(), m.deposit() - refund);
        }
        // 2. 每笔没收按"其余成员天数占比"再分配，无人可分则沉没
        for (MemberStake m : members) {
            int forfeit = forfeits.get(m.userId());
            if (forfeit <= 0) {
                continue;
            }
            long otherDaysSum = members.stream()
                    .filter(o -> !o.userId().equals(m.userId()))
                    .mapToLong(o -> Math.min(Math.max(o.days(), 0), totalDays))
                    .sum();
            if (otherDaysSum <= 0) {
                sunk[0] += forfeit;
                continue;
            }
            int allocated = 0;
            for (MemberStake other : members) {
                if (other.userId().equals(m.userId())) {
                    continue;
                }
                int otherDays = Math.min(Math.max(other.days(), 0), totalDays);
                if (otherDays == 0) {
                    continue;
                }
                int share = forfeit * otherDays / (int) otherDaysSum;
                if (share > 0) {
                    shares.merge(other.userId(), share, Integer::sum);
                    allocated += share;
                }
            }
            sunk[0] += forfeit - allocated;
        }
        return new SettlementResult(refunds, shares, sunk[0]);
    }
}
