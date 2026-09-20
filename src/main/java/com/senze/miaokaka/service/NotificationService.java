package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.entity.UserNotification;
import com.senze.miaokaka.model.vo.NotificationVO;

/**
 * 用户通知服务（落库持久，通用 type 预留扩展）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface NotificationService extends IService<UserNotification> {

    /**
     * 发一条通知（供各业务在事务内调用，与业务变更原子）
     */
    void notify(Long userId, int type, String title, String content, Long refId);

    /**
     * 我的通知分页（id 倒序）
     */
    Page<NotificationVO> pageMy(long current, long pageSize, Long userId);

    /**
     * 我的未读数（角标）
     */
    long unreadCount(Long userId);

    /**
     * 标记单条已读（归属校验：非本人通知拒绝；幂等可重复调）
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 全部标记已读
     *
     * @return 本次标记条数
     */
    int markAllRead(Long userId);
}
