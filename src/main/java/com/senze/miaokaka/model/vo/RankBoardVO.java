package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 连击排行榜视图：Top 50 + 当前用户排名
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class RankBoardVO implements Serializable {

    private List<RankItemVO> list;

    /**
     * 当前用户排名（1 起）；零连击为 null（暂无排名）；
     * Top 50 内取榜单名次，50 外按同口径实时补算
     */
    private Integer myRank;

    private static final long serialVersionUID = 1L;
}
