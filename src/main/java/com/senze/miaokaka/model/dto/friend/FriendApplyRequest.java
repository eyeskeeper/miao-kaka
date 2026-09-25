package com.senze.miaokaka.model.dto.friend;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友申请请求
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class FriendApplyRequest implements Serializable {

    /**
     * 目标用户id
     */
    @NotNull(message = "目标用户id不能为空")
    private Long targetUserId;

    private static final long serialVersionUID = 1L;
}
