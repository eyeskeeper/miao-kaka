package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.constant.AnnouncementConstant;
import com.senze.miaokaka.constant.NotificationConstant;
import com.senze.miaokaka.mapper.AnnouncementMapper;
import com.senze.miaokaka.mapper.UserMapper;
import com.senze.miaokaka.mapper.UserNotificationMapper;
import com.senze.miaokaka.model.entity.Announcement;
import com.senze.miaokaka.model.entity.User;
import com.senze.miaokaka.model.entity.UserNotification;
import com.senze.miaokaka.model.vo.AnnouncementVO;
import com.senze.miaokaka.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统公告服务实现。
 * 发布即广播：公告落库为事实源（ref_id 关联），同时复制通知行（type=3）到每个未删除用户的通知中心。
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementService {

    private final UserMapper userMapper;

    private final UserNotificationMapper userNotificationMapper;

    private final TransactionTemplate transactionTemplate;

    @Override
    public Map<String, Object> createAndBroadcast(Long creatorId, String title, String content) {
        List<User> allUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getIsDelete, 0));

        Long announcementId = transactionTemplate.execute(status -> {
            Announcement announcement = new Announcement();
            announcement.setCreatorId(creatorId);
            announcement.setTitle(title.trim());
            announcement.setContent(content.trim());
            announcement.setCreateTime(new Date());
            save(announcement);
            // 广播：复制通知行到每个未删除用户（ref_id 关联公告，便于统计送达）
            Date now = new Date();
            List<UserNotification> notifications = new ArrayList<>(allUsers.size());
            for (User u : allUsers) {
                UserNotification n = new UserNotification();
                n.setUserId(u.getId());
                n.setType(NotificationConstant.TYPE_ANNOUNCEMENT);
                n.setTitle(title.trim());
                n.setContent(content.trim());
                n.setRefId(announcement.getId());
                n.setIsRead(0);
                n.setCreateTime(now);
                notifications.add(n);
            }
            if (!notifications.isEmpty()) {
                notifications.forEach(userNotificationMapper::insert);
            }
            return announcement.getId();
        });
        log.info("公告 {}「{}」已广播给 {} 位用户", announcementId, title, allUsers.size());

        Map<String, Object> result = new HashMap<>();
        result.put("announcementId", announcementId);
        result.put("delivered", allUsers.size());
        return result;
    }

    @Override
    public Page<AnnouncementVO> history(long current, long pageSize) {
        Page<Announcement> page = page(new Page<>(current, Math.min(pageSize, AnnouncementConstant.HISTORY_LIMIT)),
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getId));
        Map<Long, User> creators = page.getRecords().isEmpty() ? Map.of()
                : userMapper.selectBatchIds(page.getRecords().stream()
                        .map(Announcement::getCreatorId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, a -> a));
        Page<AnnouncementVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(a -> {
            AnnouncementVO vo = new AnnouncementVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setContent(a.getContent());
            User creator = creators.get(a.getCreatorId());
            vo.setCreatorName(creator == null ? "" : creator.getUserName());
            vo.setCreateTime(a.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }
}
