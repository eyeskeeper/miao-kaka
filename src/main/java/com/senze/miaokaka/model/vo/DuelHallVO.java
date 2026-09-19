package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 死斗大厅条目（招募中/进行中的全量分页列表，轻量脱敏版——不带成员明细）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class DuelHallVO implements Serializable {

    private Long id;

    private String duelName;

    private String duelDesc;

    private Long leaderId;

    private String leaderName;

    /**
     * 加入模式 (0:自由加入, 1:审批加入)
     */
    private Integer joinMode;

    private Integer depositPerMember;

    private Integer totalDays;

    private Integer memberCount;

    /**
     * 0:招募中, 1:进行中
     */
    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * 我与该局的关系：leader / member / applicant（有待审申请）/ null（陌生人）
     */
    private String myRelation;

    private static final long serialVersionUID = 1L;
}
