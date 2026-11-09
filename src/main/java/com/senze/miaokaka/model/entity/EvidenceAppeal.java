package com.senze.miaokaka.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 凭证上诉（互审局驳回后 48h 内可上诉，组长仲裁，超 48h 未仲裁自动改判通过）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@TableName(value = "evidence_appeal")
@Data
public class EvidenceAppeal implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 凭证id（每凭证限一条上诉，唯一键兜底）
     */
    private Long evidenceId;

    private Long duelId;

    /**
     * 上诉人（凭证提交者本人）
     */
    private Long appealerId;

    /**
     * 上诉理由
     */
    private String reason;

    /**
     * 状态 (0:待组长仲裁, 1:改判通过, 2:维持驳回, 3:超时自动改判通过)
     */
    private Integer status;

    /**
     * 处理人（组长/admin；超时自动改判为空）
     */
    private Long handlerId;

    /**
     * 处理备注
     */
    private String handleRemark;

    /**
     * 处理时间
     */
    private Date handleTime;

    /**
     * 提交时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
