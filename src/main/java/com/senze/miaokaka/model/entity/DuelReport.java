package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 死斗举报（组长局驳回直报；互审局上诉维持后可报；每人每局限一次，admin 终裁）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "duel_report")
@Data
public class DuelReport implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被举报死斗id
     */
    private Long duelId;

    /**
     * 举报人
     */
    private Long reporterId;

    /**
     * 关联凭证id
     */
    private Long evidenceId;

    /**
     * 举报理由
     */
    private String reason;

    /**
     * 状态 (0:待处理, 1:改判通过, 2:维持原判, 3:无法处理（局已结算/成员已离局）)
     */
    private Integer status;

    /**
     * 处理admin
     */
    private Long adminId;

    /**
     * 处理备注（含误判留痕：原审核人及连带组长）
     */
    private String handleRemark;

    /**
     * 处理时间
     */
    private Date handleTime;

    /**
     * 举报时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
