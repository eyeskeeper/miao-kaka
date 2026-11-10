package com.senze.miaokaka.config;

import com.senze.miaokaka.service.DuelBattleService;
import com.senze.miaokaka.service.DuelAppealService;
import com.senze.miaokaka.service.DuelImpeachmentService;
import com.senze.miaokaka.service.DuelService;
import com.senze.miaokaka.service.DuelSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 死斗定时任务：到点开赛 + 到期结算 + 弹劾判负 + 待审超时自动通过（详情/列表访问时有 lazy 兜底）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DuelScheduler {

    private final DuelService duelService;

    private final DuelSettlementService duelSettlementService;

    private final DuelImpeachmentService duelImpeachmentService;

    private final DuelBattleService duelBattleService;

    private final DuelAppealService duelAppealService;

    /**
     * 每日 00:20（北京时间，随服务器时区）开赛扫描
     */
    @Scheduled(cron = "0 20 0 * * ?")
    public void startDuels() {
        log.info("定时任务：死斗开赛扫描开始");
        duelService.startDueDuels();
    }

    /**
     * 每日 00:40 结算扫描
     */
    @Scheduled(cron = "0 40 0 * * ?")
    public void settleDuels() {
        log.info("定时任务：死斗结算扫描开始");
        duelSettlementService.settleAllDue();
    }

    /**
     * 每 10 分钟扫期：过期未过半的弹劾判负（详情访问/投票时有惰性兜底）
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void sweepImpeachments() {
        int resolved = duelImpeachmentService.resolveExpired();
        if (resolved > 0) {
            log.info("定时任务：判负过期弹劾 {} 场", resolved);
        }
    }

    /**
     * 每小时整点：当日未审完的待审凭证自动通过（SLA 当日化）；
     * 待仲裁上诉超 48 小时未处理自动改判通过（不作为的代价由不作为方承担）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoApproveOverdueEvidence() {
        int approved = duelBattleService.autoApproveOverdue();
        if (approved > 0) {
            log.info("定时任务：超时自动通过待审凭证 {} 条", approved);
        }
        int overturned = duelAppealService.resolveOverdue();
        if (overturned > 0) {
            log.info("定时任务：超时自动改判上诉 {} 条", overturned);
        }
    }
}
