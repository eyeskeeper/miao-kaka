package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.constant.CheckInConstant;
import com.senze.miaokaka.constant.FriendConstant;
import com.senze.miaokaka.constant.NotificationConstant;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.exception.BusinessException;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.CheckInLikeMapper;
import com.senze.miaokaka.mapper.CheckInPlanMapper;
import com.senze.miaokaka.mapper.CheckInRecordMapper;
import com.senze.miaokaka.mapper.CatSpiritMapper;
import com.senze.miaokaka.mapper.UserFriendMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.model.entity.CatSpirit;
import com.senze.miaokaka.model.entity.CheckInLike;
import com.senze.miaokaka.model.entity.CheckInPlan;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.entity.UserFriend;
import com.senze.miaokaka.model.vo.CheckInCalendarVO;
import com.senze.miaokaka.model.vo.FriendApplicationVO;
import com.senze.miaokaka.model.vo.FriendCatVO;
import com.senze.miaokaka.model.vo.FriendFeedItemVO;
import com.senze.miaokaka.model.vo.FriendRankItemVO;
import com.senze.miaokaka.model.vo.FriendSearchVO;
import com.senze.miaokaka.model.vo.FriendVO;
import com.senze.miaokaka.service.FriendService;
import com.senze.miaokaka.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 好友服务实现。
 * 申请-同意制；同意后写入双向两行（好友判定免 OR）；
 * 围观/点赞/动态/排行仅好友可见；被赞者小鱼干 +1（每日点赞限 5 次、同记录仅一次）。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendServiceImpl extends ServiceImpl<UserFriendMapper, UserFriend> implements FriendService {

    private final UserFriendMapper userFriendMapper;

    private final UserMapper userMapper;

    private final CheckInRecordMapper checkInRecordMapper;

    private final CheckInPlanMapper checkInPlanMapper;

    private final CatSpiritMapper catSpiritMapper;

    private final CheckInLikeMapper checkInLikeMapper;

    private final NotificationService notificationService;

    // region 好友关系

    @Override
    public void apply(Long userId, Long targetUserId) {
        ThrowUtils.throwIf(userId.equals(targetUserId), ErrorCode.PARAMS_ERROR, "不能添加自己为好友");
        User target = userMapper.selectById(targetUserId);
        ThrowUtils.throwIf(target == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        if (isFriend(userId, targetUserId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "你们已经是好友了");
        }
        long pendingEither = userFriendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .and(w -> w.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, targetUserId))
                .or()
                .eq(UserFriend::getUserId, targetUserId).eq(UserFriend::getFriendId, userId));
        ThrowUtils.throwIf(pendingEither > 0, ErrorCode.OPERATION_ERROR, "已存在好友申请或好友关系，请勿重复添加");
        UserFriend request = new UserFriend();
        request.setUserId(userId);
        request.setFriendId(targetUserId);
        request.setStatus(FriendConstant.STATUS_PENDING);
        try {
            userFriendMapper.insert(request);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已存在好友申请，请勿重复添加");
        }
        String applicantName = userNameOf(userId);
        notificationService.notify(targetUserId, NotificationConstant.TYPE_FRIEND_APPLY,
                "好友申请", "「" + applicantName + "」想加你为好友，去消息中心处理吧", userId);
    }

    @Override
    public List<FriendApplicationVO> applications(Long userId) {
        List<UserFriend> rows = userFriendMapper.selectList(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getFriendId, userId)
                .eq(UserFriend::getStatus, FriendConstant.STATUS_PENDING)
                .orderByAsc(UserFriend::getId));
        Map<Long, User> users = usersOf(rows.stream().map(UserFriend::getUserId).toList());
        return rows.stream().map(r -> {
            FriendApplicationVO vo = new FriendApplicationVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            User applicant = users.get(r.getUserId());
            if (applicant != null) {
                vo.setUserName(applicant.getUserName());
                vo.setUserAvatar(applicant.getUserAvatar());
            }
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).toList();
    }

    @Override
    public void agree(Long userId, Long applicationId) {
        UserFriend row = userFriendMapper.selectById(applicationId);
        ThrowUtils.throwIf(row == null || !row.getFriendId().equals(userId),
                ErrorCode.NOT_FOUND_ERROR, "申请不存在");
        ThrowUtils.throwIf(row.getStatus() != FriendConstant.STATUS_PENDING,
                ErrorCode.OPERATION_ERROR, "该申请已处理过");
        row.setStatus(FriendConstant.STATUS_AGREED);
        row.setAgreeTime(new java.util.Date());
        userFriendMapper.updateById(row);
        // 反向行（好友判定查 user_id=我 方向）
        UserFriend reverse = new UserFriend();
        reverse.setUserId(userId);
        reverse.setFriendId(row.getUserId());
        reverse.setStatus(FriendConstant.STATUS_AGREED);
        reverse.setAgreeTime(row.getAgreeTime());
        try {
            userFriendMapper.insert(reverse);
        } catch (DuplicateKeyException e) {
            // 已存在同向好友行（理论上不可能），忽略
        }
        notificationService.notify(row.getUserId(), NotificationConstant.TYPE_FRIEND_AGREE,
                "好友申请已通过", "「" + userNameOf(userId) + "」已通过你的好友申请，快去围观吧", userId);
    }

    @Override
    public void reject(Long userId, Long applicationId) {
        UserFriend row = userFriendMapper.selectById(applicationId);
        ThrowUtils.throwIf(row == null || !row.getFriendId().equals(userId),
                ErrorCode.NOT_FOUND_ERROR, "申请不存在");
        ThrowUtils.throwIf(row.getStatus() != FriendConstant.STATUS_PENDING,
                ErrorCode.OPERATION_ERROR, "该申请已处理过");
        userFriendMapper.deleteById(row.getId());
    }

    @Override
    public List<FriendVO> listFriends(Long userId) {
        List<Long> friendIds = friendIdsOf(userId);
        if (friendIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = usersOf(friendIds);
        List<FriendVO> vos = new ArrayList<>();
        for (Long fid : friendIds) {
            User u = users.get(fid);
            if (u == null) {
                continue;
            }
            FriendVO vo = new FriendVO();
            vo.setUserId(u.getId());
            vo.setUserName(u.getUserName());
            vo.setUserAvatar(u.getUserAvatar());
            vo.setCurrentStreak(u.getCurrentStreak() == null ? 0 : u.getCurrentStreak());
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public void removeFriend(Long userId, Long friendUserId) {
        if (!isFriend(userId, friendUserId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "你们还不是好友");
        }
        userFriendMapper.delete(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendId, friendUserId));
        userFriendMapper.delete(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, friendUserId).eq(UserFriend::getFriendId, userId));
    }

    @Override
    public FriendSearchVO search(String keyword) {
        ThrowUtils.throwIf(com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(keyword),
                ErrorCode.PARAMS_ERROR, "请输入账号或用户 id");
        User user = null;
        if (keyword.chars().allMatch(Character::isDigit)) {
            user = userMapper.selectById(Long.valueOf(keyword));
        }
        if (user == null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUserAccount, keyword.trim())
                    .last("limit 1"));
        }
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        FriendSearchVO vo = new FriendSearchVO();
        vo.setUserId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserAvatar(user.getUserAvatar());
        return vo;
    }

    // endregion

    // region 排行 / 动态 / 围观 / 点赞

    @Override
    public List<FriendRankItemVO> rank(Long userId) {
        List<Long> ids = friendIdsOf(userId);
        if (!ids.contains(userId)) {
            ids.add(userId);
        }
        Map<Long, User> users = usersOf(ids);
        return ids.stream()
                .filter(users::containsKey)
                .map(id -> {
                    User u = users.get(id);
                    FriendRankItemVO vo = new FriendRankItemVO();
                    vo.setUserId(id);
                    vo.setUserName(u.getUserName());
                    vo.setUserAvatar(u.getUserAvatar());
                    vo.setCurrentStreak(u.getCurrentStreak() == null ? 0 : u.getCurrentStreak());
                    vo.setIsMe(id.equals(userId));
                    return vo;
                })
                .sorted(Comparator.comparingInt(FriendRankItemVO::getCurrentStreak).reversed()
                        .thenComparing(FriendRankItemVO::getUserId))
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendFeedItemVO> feed(Long userId) {
        List<Long> friendIds = friendIdsOf(userId);
        if (friendIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        LocalDate since = today.minusDays(FriendConstant.FEED_LOOKBACK_DAYS);
        List<CheckInRecord> records = checkInRecordMapper.selectList(
                new LambdaQueryWrapper<CheckInRecord>()
                        .in(CheckInRecord::getUserId, friendIds)
                        .in(CheckInRecord::getStatus, CheckInConstant.RECORD_STATUS_NORMAL,
                                CheckInConstant.RECORD_STATUS_MAKEUP)
                        .ge(CheckInRecord::getCheckInDate, since)
                        .orderByDesc(CheckInRecord::getCheckInDate)
                        .orderByDesc(CheckInRecord::getRecordId)
                        .last("limit " + FriendConstant.FEED_MAX_SIZE));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = usersOf(records.stream().map(CheckInRecord::getUserId).distinct().toList());
        List<Long> planIds = records.stream().map(CheckInRecord::getPlanId).distinct().toList();
        Map<Long, CheckInPlan> plans = checkInPlanMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(CheckInPlan::getId, Function.identity()));
        List<Long> recordIds = records.stream().map(CheckInRecord::getRecordId).toList();
        Map<Long, Long> likeCounts = checkInLikeMapper.selectList(new LambdaQueryWrapper<CheckInLike>()
                        .in(CheckInLike::getRecordId, recordIds)).stream()
                .collect(Collectors.groupingBy(CheckInLike::getRecordId, Collectors.counting()));
        Set<Long> myLiked = checkInLikeMapper.selectList(new LambdaQueryWrapper<CheckInLike>()
                        .in(CheckInLike::getRecordId, recordIds)
                        .eq(CheckInLike::getLikerId, userId)).stream()
                .map(CheckInLike::getRecordId).collect(Collectors.toSet());

        List<FriendFeedItemVO> vos = new ArrayList<>();
        for (CheckInRecord r : records) {
            FriendFeedItemVO vo = new FriendFeedItemVO();
            vo.setRecordId(r.getRecordId());
            vo.setFriendId(r.getUserId());
            User u = users.get(r.getUserId());
            if (u != null) {
                vo.setFriendName(u.getUserName());
                vo.setFriendAvatar(u.getUserAvatar());
            }
            CheckInPlan plan = plans.get(r.getPlanId());
            vo.setPlanName(plan == null ? "" : plan.getPlanName());
            vo.setCheckInDate(r.getCheckInDate());
            vo.setStatus(r.getStatus());
            vo.setLikeCount(likeCounts.getOrDefault(r.getRecordId(), 0L).intValue());
            vo.setLikedByMe(myLiked.contains(r.getRecordId()));
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public FriendCatVO friendCats(Long userId, Long friendId) {
        requireFriend(userId, friendId);
        User friend = userMapper.selectById(friendId);
        ThrowUtils.throwIf(friend == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        FriendCatVO vo = new FriendCatVO();
        vo.setFriendId(friendId);
        vo.setFriendName(friend.getUserName());
        List<CheckInPlan> plans = checkInPlanMapper.selectList(new LambdaQueryWrapper<CheckInPlan>()
                .eq(CheckInPlan::getUserId, friendId)
                .eq(CheckInPlan::getStatus, CheckInConstant.PLAN_STATUS_ACTIVE));
        List<FriendCatVO.CatItem> cats = new ArrayList<>();
        for (CheckInPlan plan : plans) {
            CatSpirit cat = catSpiritMapper.selectOne(new LambdaQueryWrapper<CatSpirit>()
                    .eq(CatSpirit::getPlanId, plan.getId())
                    .last("limit 1"));
            if (cat == null) {
                continue;
            }
            FriendCatVO.CatItem item = new FriendCatVO.CatItem();
            item.setPlanId(plan.getId());
            item.setPlanName(plan.getPlanName());
            item.setCatName(cat.getCatName());
            item.setLevel(cat.getLevel());
            item.setBossName(cat.getBossName());
            item.setTotalBossDefeated(cat.getTotalBossDefeated() == null ? 0 : cat.getTotalBossDefeated());
            cats.add(item);
        }
        vo.setCats(cats);
        return vo;
    }

    @Override
    public CheckInCalendarVO friendCalendar(Long userId, Long friendId, Long planId, String month) {
        requireFriend(userId, friendId);
        CheckInPlan plan = checkInPlanMapper.selectById(planId);
        ThrowUtils.throwIf(plan == null || !plan.getUserId().equals(friendId),
                ErrorCode.NOT_FOUND_ERROR, "该计划不属于这位好友");
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        java.time.YearMonth yearMonth;
        try {
            yearMonth = com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(month)
                    ? java.time.YearMonth.from(today) : java.time.YearMonth.parse(month);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "月份格式应为 yyyy-MM");
        }
        List<CheckInRecord> records = checkInRecordMapper.selectList(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getPlanId, planId)
                        .ge(CheckInRecord::getCheckInDate, yearMonth.atDay(1))
                        .le(CheckInRecord::getCheckInDate, yearMonth.atEndOfMonth())
                        .in(CheckInRecord::getStatus, CheckInConstant.RECORD_STATUS_NORMAL,
                                CheckInConstant.RECORD_STATUS_MAKEUP));
        CheckInCalendarVO vo = new CheckInCalendarVO();
        vo.setPlanId(planId);
        vo.setMonth(yearMonth.toString());
        List<CheckInCalendarVO.DayRecord> days = records.stream().map(r -> {
            CheckInCalendarVO.DayRecord dr = new CheckInCalendarVO.DayRecord();
            dr.setDate(r.getCheckInDate());
            dr.setStatus(r.getStatus());
            dr.setRemark(r.getRemark());
            return dr;
        }).toList();
        vo.setDays(days);
        return vo;
    }

    @Override
    public long like(Long userId, Long recordId) {
        CheckInRecord record = checkInRecordMapper.selectById(recordId);
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "打卡记录不存在");
        Long targetId = record.getUserId();
        ThrowUtils.throwIf(targetId.equals(userId), ErrorCode.OPERATION_ERROR, "不能给自己的打卡点赞");
        if (!isFriend(userId, targetId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅好友可以点赞");
        }
        LocalDate today = LocalDate.now(CheckInConstant.BIZ_ZONE);
        long todayLikes = checkInLikeMapper.selectCount(new LambdaQueryWrapper<CheckInLike>()
                .eq(CheckInLike::getLikerId, userId)
                .eq(CheckInLike::getLikeDate, today));
        ThrowUtils.throwIf(todayLikes >= FriendConstant.DAILY_LIKE_LIMIT,
                ErrorCode.OPERATION_ERROR, "今日点赞次数已用完（每日 " + FriendConstant.DAILY_LIKE_LIMIT + " 次）");
        CheckInLike like = new CheckInLike();
        like.setRecordId(recordId);
        like.setLikerId(userId);
        like.setTargetId(targetId);
        like.setLikeDate(today);
        try {
            checkInLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经点过赞啦");
        }
        // 被赞者小鱼干 +1（原子）
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, targetId)
                .setSql("dried_fish = dried_fish + 1"));
        long likeCount = checkInLikeMapper.selectCount(new LambdaQueryWrapper<CheckInLike>()
                .eq(CheckInLike::getRecordId, recordId));
        return likeCount;
    }

    // endregion

    // region 内部工具

    public boolean isFriend(Long a, Long b) {
        return userFriendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, a)
                .eq(UserFriend::getFriendId, b)
                .eq(UserFriend::getStatus, FriendConstant.STATUS_AGREED)) > 0;
    }

    private List<Long> friendIdsOf(Long userId) {
        return userFriendMapper.selectList(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getStatus, FriendConstant.STATUS_AGREED))
                .stream().map(UserFriend::getFriendId).collect(Collectors.toList());
    }

    private Map<Long, User> usersOf(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private String userNameOf(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? "" : user.getUserName();
    }

    private void requireFriend(Long userId, Long friendId) {
        ThrowUtils.throwIf(!isFriend(userId, friendId), ErrorCode.NO_AUTH_ERROR, "仅好友可以围观");
    }

    // endregion
}
