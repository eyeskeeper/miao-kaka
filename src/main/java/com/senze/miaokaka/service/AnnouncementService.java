package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.entity.Announcement;
import com.senze.miaokaka.model.vo.AnnouncementVO;

import java.util.Map;

/**
 * 系统公告服务（admin 发布 → 广播复制到全员通知中心）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface AnnouncementService extends IService<Announcement> {

    /**
     * 发布公告并广播：复制一条通知到每个未删除用户的通知中心
     *
     * @return {announcementId, delivered}
     */
    Map<String, Object> createAndBroadcast(Long creatorId, String title, String content);

    /**
     * 公告历史（最新在前）
     */
    Page<AnnouncementVO> history(long current, long pageSize);
}
