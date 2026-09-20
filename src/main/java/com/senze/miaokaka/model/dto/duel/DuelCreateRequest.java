package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建习惯死斗请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class DuelCreateRequest implements Serializable {

    @NotBlank(message = "死斗名称不能为空")
    @Size(max = 128, message = "死斗名称最长 128 字")
    private String duelName;

    @Size(max = 512, message = "死斗描述最长 512 字")
    private String duelDesc;

    /**
     * 每日任务清单（可选，最多 5 项；成员影子计划将复制此清单，可由 POST /ai/plan/draft 草稿确认而来）
     */
    @Size(max = 5, message = "每日任务最多 5 项")
    private List<String> dailyTasks;

    /**
     * 每人押金（喵币）
     */
    @NotNull(message = "押金不能为空")
    @Min(value = 100, message = "押金最低 100 喵币")
    @Max(value = 5000, message = "押金最高 5000 喵币")
    private Integer depositPerMember;

    /**
     * 挑战天数（3~365）
     */
    @NotNull(message = "挑战天数不能为空")
    @Min(value = 3, message = "挑战天数最少 3 天")
    @Max(value = 365, message = "挑战天数最多 365 天")
    private Integer totalDays;

    /**
     * 开始日期 yyyy-MM-dd（不传默认明天）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式应为 yyyy-MM-dd")
    private String startDate;

    /**
     * 加入模式 (0:自由加入, 1:审批加入)
     */
    @Min(value = 0, message = "加入模式不合法")
    @Max(value = 1, message = "加入模式不合法")
    private Integer joinMode = 0;

    /**
     * 人数上限（2~50，含组长；满员后无法再加入/申请）
     */
    @NotNull(message = "人数上限不能为空")
    @Min(value = 2, message = "人数上限最少 2 人")
    @Max(value = 50, message = "人数上限最多 50 人")
    private Integer maxMembers;

    /**
     * 是否隐藏（true=不在招募大厅出现，仅可通过组号/邀请海报发现）
     */
    private Boolean hidden;

    private static final long serialVersionUID = 1L;
}
