package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.entity.UserItem;
import com.senze.miaokaka.model.vo.BagItemVO;
import com.senze.miaokaka.model.vo.MallItemVO;

import java.util.List;

/**
 * 积分商城服务（商品目录 + 背包 + 购买/消耗）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface MallService extends IService<UserItem> {

    /**
     * 商品目录（含我的持有数量）
     */
    List<MallItemVO> catalog(Long userId);

    /**
     * 我的背包
     */
    List<BagItemVO> bag(Long userId);

    /**
     * 购买：原子扣积分 + 背包 +1（积分不足 40000）
     *
     * @return 购买后剩余积分与持有数量
     */
    com.senze.miaokaka.model.vo.MallBuyResultVO buy(Long userId, String itemCode);

    /**
     * 消耗一张道具（条件更新 quantity>0，防负数）；无货返回 false
     */
    boolean consumeItem(Long userId, String itemCode);

    /**
     * 小鱼干兑积分（1:1，原子扣减 dried_fish，不足拒绝）
     *
     * @return 兑换后剩余小鱼干
     */
    int exchangeFish(Long userId, int fish);
}
