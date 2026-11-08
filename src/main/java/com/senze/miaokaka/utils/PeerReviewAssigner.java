package com.senze.miaokaka.utils;

import com.senze.miaokaka.model.entity.DuelMember;

import java.util.Comparator;
import java.util.List;

/**
 * 组内互审派单：修正版错位环。
 * 成员按 userId 升序编号 0..N-1；当日偏移 k = dayIndex % (N-1) + 1，恒落在 1..N-1，
 * 审核员编号 = (提交者编号 + k) % N——k 不为 0 也不为 N，故审核员永不等于提交者本人；
 * 每人每天恰审一份、被审一份，每 N-1 天与其余每位成员各轮到一次。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public final class PeerReviewAssigner {

    private PeerReviewAssigner() {
    }

    /**
     * 按提交日算审核员。
     *
     * @param members     在册成员（会被就地按 userId 升序重排）
     * @param submitterId 提交凭证的用户id
     * @param dayIndex    开始日到提交日的天数差（0 = 首日）
     * @return 审核员成员；成员不足 2 人或不包含提交者时返回 null（调用方回退人工/自动通过兜底）
     */
    public static DuelMember assigneeOf(List<DuelMember> members, Long submitterId, long dayIndex) {
        if (members == null || members.size() < 2 || submitterId == null) {
            return null;
        }
        members.sort(Comparator.comparing(DuelMember::getUserId));
        int n = members.size();
        int submitterIndex = -1;
        for (int i = 0; i < n; i++) {
            if (members.get(i).getUserId().equals(submitterId)) {
                submitterIndex = i;
                break;
            }
        }
        if (submitterIndex < 0) {
            return null;
        }
        int offset = (int) (dayIndex % (n - 1)) + 1;
        return members.get((submitterIndex + offset) % n);
    }
}
