package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.dto.plan.TaskToggleRequest;
import com.senze.miaokaka.model.entity.CheckInRecord;
import com.senze.miaokaka.model.vo.CheckInCalendarVO;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.MakeupResultVO;
import com.senze.miaokaka.model.vo.TaskToggleVO;

/**
 * 打卡记录服务（含打卡事件引擎与补卡）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface CheckInRecordService extends IService<CheckInRecord> {

    /**
     * 打卡：触发一次随机事件（攻击BOSS/属性提升/暴击），返回事件过程与成长结算。
     * AI 鼓励语在事务提交后生成，失败自动降级本地语录，不影响打卡结果。
     */
    CheckInResultVO checkIn(Long userId, Long planId, String remark);

    /**
     * 补卡：扣积分、自然月限次、恢复连击；不触发事件
     */
    MakeupResultVO makeup(Long userId, Long planId, String date);

    /**
     * 某计划某月打卡日历
     */
    CheckInCalendarVO calendar(Long userId, Long planId, String month);

    /**
     * 死斗凭证审核通过后的结算入口：记录置为正常、重算连击并触发一次事件结算。
     * 返回事件结果（含猫口吻鼓励语），供审核响应展示。
     */
    CheckInResultVO settleApprovedCheckIn(Long userId, Long planId, Long recordId);

    /**
     * 每日任务勾选（部分打卡）：维护计划位图，勾满最后一项自动完成当日打卡
     * （死斗影子计划不自动打卡——需走照片凭证入口）。
     * 当日已有任意状态打卡记录后冻结勾选。
     */
    TaskToggleVO toggleTask(Long userId, Long planId, TaskToggleRequest request);
}
