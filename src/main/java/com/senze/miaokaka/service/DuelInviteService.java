package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.entity.DuelInvite;
import com.senze.miaokaka.model.vo.InviteInfoVO;
import com.senze.miaokaka.model.vo.InviteUseResultVO;
import com.senze.miaokaka.model.vo.InviteVO;

/**
 * 死斗邀请服务（生成邀请码/海报、免登录落地信息、扫码加入）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface DuelInviteService extends IService<DuelInvite> {

    /**
     * 生成/获取我的邀请：每个（成员，死斗）固定一个长期码，海报只渲染一次复用；仅招募中
     */
    InviteVO getOrCreateInvite(Long userId, Long duelId);

    /**
     * 免登录扫码落地：死斗摘要 + 邀请人 + 加入动作提示（direct/apply）
     */
    InviteInfoVO resolveInfo(String code);

    /**
     * 使用邀请码：组长码两种模式都直接入组（审批制免审）；
     * 成员码：自由制直接加入、审批制提交申请并留痕邀请人
     */
    InviteUseResultVO useInvite(Long userId, String code);
}
