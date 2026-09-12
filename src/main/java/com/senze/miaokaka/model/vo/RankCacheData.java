package com.senze.miaokaka.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 排行榜缓存包装（解决 List 泛型擦除导致的反序列化类型丢失）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class RankCacheData implements Serializable {

    private List<RankItemVO> items;

    private static final long serialVersionUID = 1L;
}
