package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.senze.miaokaka.model.dto.duel.AppealResolveRequest;
import com.senze.miaokaka.model.dto.duel.AppealSubmitRequest;
import com.senze.miaokaka.model.dto.duel.ReportResolveRequest;
import com.senze.miaokaka.model.dto.duel.ReportSubmitRequest;
import com.senze.miaokaka.model.vo.AppealPendingVO;
import com.senze.miaokaka.model.vo.CheckInResultVO;
import com.senze.miaokaka.model.vo.ReportAdminVO;

import java.util.List;

/**
 * 凭证上诉与死斗举报服务（救济通道，仅针对驳回）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface DuelAppealService {

    /**
     * 提交上诉：互审局、本人被驳回凭证、驳回后 48h 内、每凭证一次；同事务通知组长
     */
    Long appeal(Long userId, Long duelId, AppealSubmitRequest request);

    /**
     * 组长待仲裁列表（互审局组长）
     */
    List<AppealPendingVO> pendingAppeals(Long userId, Long duelId);

    /**
     * 组长仲裁：改判通过（补钱+补事件结算）/ 维持驳回（通知上诉人）
     */
    CheckInResultVO resolveAppeal(Long userId, Long duelId, AppealResolveRequest request);

    /**
     * 超时兜底：待仲裁超 48h 自动改判通过（进行中局），返回处理条数（定时任务每小时调用）
     */
    int resolveOverdue();

    /**
     * 死斗结束/解散时未决上诉批量置失效（组内救济通道随局关闭）
     */
    int invalidatePending(Long duelId);

    /**
     * 提交举报：组长局驳回直报；互审局须上诉维持后；每人每局限一次
     */
    Long report(Long userId, Long duelId, ReportSubmitRequest request);

    /**
     * admin：举报分页（关联凭证/审核/上诉链）
     */
    Page<ReportAdminVO> reportPage(long current, long pageSize, Integer status);

    /**
     * admin 终裁：改判通过（仅未结算局且提交者在册，含误判留痕）/ 维持原判 / 无法处理标记
     */
    CheckInResultVO resolveReport(Long adminId, Long reportId, ReportResolveRequest request);
}
