package com.senze.miaokaka.service;

import com.senze.miaokaka.model.dto.admin.AnnouncementCreateRequest;
import com.senze.miaokaka.model.vo.AdminStatsVO;
import com.senze.miaokaka.model.vo.AnnouncementVO;

import java.util.List;
import java.util.Map;

/**
 * 管理端服务（公告广播 + 数据看板）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface AdminService {

    /**
     * 发布公告并广播到全员通知中心
     *
     * @return {announcementId, delivered}
     */
    Map<String, Object> createAnnouncement(Long creatorId, AnnouncementCreateRequest request);

    /**
     * 公告历史（最新在前）
     */
    List<AnnouncementVO> announcements(long current, long pageSize);

    /**
     * 数据看板：用户/打卡/局数统计 + 近 7 天趋势
     */
    AdminStatsVO stats();
}
