package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.senze.miaokaka.constant.AnnouncementConstant;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.DuelConstant;
import com.senze.miaokaka.mapper.AnnouncementMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.DuelMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.dto.admin.AnnouncementCreateRequest;
import com.senze.miaokaka.model.entity.Announcement;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.Duel;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.vo.AdminStatsVO;
import com.senze.miaokaka.model.vo.AnnouncementVO;
import com.senze.miaokaka.service.AdminService;
import com.senze.miaokaka.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理端服务实现（公告委托广播服务；看板为全站只读聚合）。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AnnouncementService announcementService;

    private final AnnouncementMapper announcementMapper;

    private final UserMapper userMapper;

    private final CheckInRecordMapper checkInRecordMapper;

    private final DuelMapper duelMapper;

    @Override
    public Map<String, Object> createAnnouncement(Long creatorId, AnnouncementCreateRequest request) {
        return announcementService.createAndBroadcast(creatorId,
                request.getTitle(), request.getContent());
    }

    @Override
    public List<AnnouncementVO> announcements(long current, long pageSize) {
        return announcementService.history(current, pageSize).getRecords();
    }

    @Override
    public AdminStatsVO stats() {
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        AdminStatsVO vo = new AdminStatsVO();

        // 用户
        vo.setTotalUsers(toInt(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getIsDelete, 0))));
        vo.setTodayNewUsers(toInt(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getIsDelete, 0)
                .ge(User::getCreateTime, today.atStartOfDay()))));

        // 打卡量（全部状态记录）
        vo.setCheckinsToday(toInt(checkInRecordMapper.selectCount(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getCheckInDate, today))));
        vo.setCheckinsYesterday(toInt(checkInRecordMapper.selectCount(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getCheckInDate, yesterday))));
        vo.setCheckinsWeek(toInt(checkInRecordMapper.selectCount(new LambdaQueryWrapper<CheckInRecord>()
                .ge(CheckInRecord::getCheckInDate, weekStart)
                .le(CheckInRecord::getCheckInDate, today))));
        vo.setCheckinsTotal(toInt(checkInRecordMapper.selectCount(null)));

        // DAU：当日打卡去重人数（先取当日记录的用户列，内存去重）
        vo.setDauToday(distinctUsersOn(today));
        vo.setDauYesterday(distinctUsersOn(yesterday));

        // 局数
        vo.setDuelsRecruiting(toInt(duelMapper.selectCount(new LambdaQueryWrapper<Duel>()
                .eq(Duel::getStatus, DuelConstant.DUEL_STATUS_RECRUITING))));
        vo.setDuelsRunning(toInt(duelMapper.selectCount(new LambdaQueryWrapper<Duel>()
                .eq(Duel::getStatus, DuelConstant.DUEL_STATUS_RUNNING))));
        vo.setDuelsSettled(toInt(duelMapper.selectCount(new LambdaQueryWrapper<Duel>()
                .eq(Duel::getStatus, DuelConstant.DUEL_STATUS_SETTLED))));

        // 近 7 天趋势
        List<AdminStatsVO.TrendDayVO> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            AdminStatsVO.TrendDayVO day = new AdminStatsVO.TrendDayVO();
            day.setDate(d);
            day.setCount(toInt(checkInRecordMapper.selectCount(new LambdaQueryWrapper<CheckInRecord>()
                    .eq(CheckInRecord::getCheckInDate, d))));
            trend.add(day);
        }
        vo.setTrend7(trend);
        return vo;
    }

    private int distinctUsersOn(LocalDate date) {
        List<CheckInRecord> records = checkInRecordMapper.selectList(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getCheckInDate, date)
                        .select(CheckInRecord::getUserId));
        return (int) records.stream().map(CheckInRecord::getUserId).distinct().count();
    }

    private int toInt(long value) {
        return Math.toIntExact(value);
    }
}
