package com.senze.miaokaka.model.dto.duel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

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

    private static final long serialVersionUID = 1L;
}
