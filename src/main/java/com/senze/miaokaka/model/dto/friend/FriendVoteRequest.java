package com.senze.miaokaka.model.dto.friend;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友申请审批 / 点赞请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FriendVoteRequest implements Serializable {

    /**
     * 申请记录id（同意/拒绝用）
     */
    @NotNull(message = "申请id不能为空")
    private Long id;

    private static final long serialVersionUID = 1L;
}
