package com.senze.miaokaka.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.NotificationConstant;
import com.senze.miaokaka.mapper.CheckInPlanMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.UserNotificationMapper;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.UserNotification;
import com.senze.miaokaka.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 打卡提醒调度：每分钟扫描 remind_time 到点且当日未打卡的进行中计划，
 * 写入通知中心（type=2，拉模式；App 端本地弹窗由前端条件编译增强）。
 * 当日去重：同一计划每天最多提醒一次（按通知表当日 type=2 + ref_id 判断）。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final CheckInPlanMapper checkInPlanMapper;

    private final CheckInRecordMapper checkInRecordMapper;

    private final UserNotificationMapper userNotificationMapper;

    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * ?")
    public void remindCheckIn() {
        LocalTime now = LocalTime.now(CheckInConstant.BIZ_ZONE).truncatedTo(ChronoUnit.MINUTES);
        String hhmm = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        List<CheckInPlan> plans = checkInPlanMapper.selectList(new LambdaQueryWrapper<CheckInPlan>()
                .eq(CheckInPlan::getStatus, CheckInConstant.PLAN_STATUS_ACTIVE)
                .eq(CheckInPlan::getRemindTime, hhmm));
        for (CheckInPlan plan : plans) {
            try {
                if (hasRecordToday(plan.getUserId(), plan.getId(), today)) {
                    continue;
                }
                if (remindedToday(plan.getId(), today)) {
                    continue;
                }
                notificationService.notify(plan.getUserId(), NotificationConstant.TYPE_CHECKIN_REMIND,
                        "打卡提醒",
                        "「" + plan.getPlanName() + "」今天的打卡还没完成，喵喵在等你！",
                        plan.getId());
                log.info("打卡提醒已发：plan={} user={} remind_time={}", plan.getId(), plan.getUserId(), hhmm);
            } catch (Exception e) {
                log.error("打卡提醒处理失败：plan={}", plan.getId(), e);
            }
        }
    }

    private boolean hasRecordToday(Long userId, Long planId, LocalDate today) {
        return checkInRecordMapper.selectCount(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getUserId, userId)
                .eq(CheckInRecord::getPlanId, planId)
                .eq(CheckInRecord::getCheckInDate, today)) > 0;
    }

    private boolean remindedToday(Long planId, LocalDate today) {
        return userNotificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getType, NotificationConstant.TYPE_CHECKIN_REMIND)
                .eq(UserNotification::getRefId, planId)
                .ge(UserNotification::getCreateTime, today.atStartOfDay())) > 0;
    }
}
