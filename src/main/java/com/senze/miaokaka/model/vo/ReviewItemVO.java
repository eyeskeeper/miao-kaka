package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 待审核凭证条目（组长视角）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class ReviewItemVO implements Serializable {

    private Long recordId;

    private Long userId;

    private String userName;

    private String userAvatar;

    /**
     * 凭证图片 URL
     */
    private String imageUrl;

    private LocalDate checkInDate;

    private String remark;

    /**
     * AI建议 (空:未启用, 0:建议通过, 1:建议驳回)
     */
    private Integer aiSuggestion;

    private String aiReason;

    private static final long serialVersionUID = 1L;
}
