package com.senze.miaokaka.service;

import com.senze.miaokaka.model.dto.nudge.NudgeTemplateRequest;
import com.senze.miaokaka.model.vo.NudgeInboxVO;
import com.senze.miaokaka.model.vo.NudgeSentVO;
import com.senze.miaokaka.model.vo.NudgeTemplateVO;

/**
 * 拍一拍服务
 * 边界：模板文案入库（user.nudge_text）；拍一拍行为与消息只存 Redis（当日过期），
 * 不落数据库表
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface NudgeService {

    NudgeTemplateVO saveTemplate(Long userId, NudgeTemplateRequest request);

    NudgeTemplateVO getTemplate(Long userId);

    /**
     * 拍一下：同死斗成员互拍；文案二级链（我的模板→系统默认）；
     * 发起方每日限 5 次；对方收件箱上限 20 条（当日过期）
     */
    NudgeSentVO nudge(Long userId, Long duelId, Long targetUserId);

    /**
     * 我的待收拍一拍（读取即消费）
     */
    NudgeInboxVO mine(Long userId);
}
