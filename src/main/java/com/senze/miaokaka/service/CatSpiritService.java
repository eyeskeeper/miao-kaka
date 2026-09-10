package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.entity.CatSpirit;
import com.senze.miaokaka.model.vo.CatVO;

/**
 * 猫精灵服务
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface CatSpiritService extends IService<CatSpirit> {

    /**
     * 为新计划生成一只猫（类型随机、随机名库命名、初始属性、一级 BOSS 满血待战）
     */
    CatSpirit createCatForPlan(Long planId);

    CatSpirit getByPlanId(Long planId);

    /**
     * 改名（归属校验由调用方完成）
     */
    boolean rename(Long catId, String newName);

    CatVO toCatVO(CatSpirit cat);
}
