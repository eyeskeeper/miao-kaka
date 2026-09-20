package com.senze.miaokaka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.common.ErrorCode;
import com.senze.miaokaka.exception.ThrowUtils;
import com.senze.miaokaka.mapper.UserNotificationMapper;
import com.senze.miaokaka.model.entity.UserNotification;
import com.senze.miaokaka.model.vo.NotificationVO;
import com.senze.miaokaka.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户通知服务实现
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
@Slf4j
public class NotificationServiceImpl extends ServiceImpl<UserNotificationMapper, UserNotification>
        implements NotificationService {

    @Override
    public void notify(Long userId, int type, String title, String content, Long refId) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRefId(refId);
        notification.setIsRead(0);
        save(notification);
    }

    @Override
    public Page<NotificationVO> pageMy(long current, long pageSize, Long userId) {
        Page<UserNotification> page = page(new Page<>(current, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .orderByDesc(UserNotification::getId));
        Page<NotificationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(n -> {
            NotificationVO vo = new NotificationVO();
            vo.setId(n.getId());
            vo.setType(n.getType());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setRefId(n.getRefId());
            vo.setIsRead(n.getIsRead() != null && n.getIsRead() == 1);
            vo.setCreateTime(n.getCreateTime());
            return vo;
        }).toList());
        return voPage;
    }

    @Override
    public long unreadCount(Long userId) {
        return count(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0));
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        UserNotification notification = getOne(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getUserId, userId));
        ThrowUtils.throwIf(notification == null, ErrorCode.OPERATION_ERROR, "通知不存在");
        if (notification.getIsRead() != null && notification.getIsRead() == 1) {
            // 幂等：已读重复调用静默成功
            return;
        }
        update(new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1));
    }

    @Override
    public int markAllRead(Long userId) {
        List<UserNotification> unread = list(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .select(UserNotification::getId));
        for (UserNotification notification : unread) {
            update(new LambdaUpdateWrapper<UserNotification>()
                    .eq(UserNotification::getId, notification.getId())
                    .set(UserNotification::getIsRead, 1));
        }
        return unread.size();
    }
}
