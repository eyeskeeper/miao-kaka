package com.senze.miaokaka.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.senze.miaokaka.constant.GameConstants;
import com.senze.miaokaka.constant.NameLibraryConstant;
import com.senze.miaokaka.mapper.CatSpiritMapper;
import com.senze.miaokaka.model.entity.CatSpirit;
import com.senze.miaokaka.model.vo.CatVO;
import com.senze.miaokaka.service.CatSpiritService;
import org.springframework.stereotype.Service;

/**
 * 猫精灵服务实现
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Service
public class CatSpiritServiceImpl extends ServiceImpl<CatSpiritMapper, CatSpirit> implements CatSpiritService {

    @Override
    public CatSpirit createCatForPlan(Long planId) {
        CatSpirit cat = new CatSpirit();
        cat.setPlanId(planId);
        cat.setCatName(NameLibraryConstant.randomCatName());
        cat.setCatType(RandomUtil.randomInt(0, 3));
        cat.setLevel(1);
        cat.setExperience(0);
        cat.setAttack(GameConstants.CAT_INIT_ATTACK);
        cat.setDefense(GameConstants.CAT_INIT_DEFENSE);
        cat.setMaxHp(GameConstants.CAT_INIT_MAX_HP);
        cat.setCurrentHp(GameConstants.CAT_INIT_MAX_HP);
        cat.setBossLevel(1);
        cat.setBossName(NameLibraryConstant.randomBossName());
        int firstBossHp = bossMaxHp(1);
        cat.setBossMaxHp(firstBossHp);
        cat.setBossHp(firstBossHp);
        cat.setTotalBossDefeated(0);
        save(cat);
        return cat;
    }

    @Override
    public CatSpirit getByPlanId(Long planId) {
        return getOne(new LambdaQueryWrapper<CatSpirit>().eq(CatSpirit::getPlanId, planId));
    }

    @Override
    public boolean rename(Long catId, String newName) {
        CatSpirit cat = getById(catId);
        if (cat == null) {
            return false;
        }
        cat.setCatName(newName);
        return updateById(cat);
    }

    @Override
    public CatVO toCatVO(CatSpirit cat) {
        CatVO vo = new CatVO();
        vo.setId(cat.getId());
        vo.setPlanId(cat.getPlanId());
        vo.setCatName(cat.getCatName());
        vo.setCatAvatar(cat.getCatAvatar());
        vo.setCatType(cat.getCatType());
        vo.setLevel(cat.getLevel());
        vo.setExperience(cat.getExperience());
        vo.setExpToNextLevel(expToNextLevel(cat.getLevel()));
        vo.setAttack(cat.getAttack());
        vo.setDefense(cat.getDefense());
        vo.setMaxHp(cat.getMaxHp());
        vo.setCurrentHp(cat.getCurrentHp());
        vo.setBossLevel(cat.getBossLevel());
        vo.setBossName(cat.getBossName());
        vo.setBossHp(cat.getBossHp());
        vo.setBossMaxHp(cat.getBossMaxHp());
        vo.setTotalBossDefeated(cat.getTotalBossDefeated());
        vo.setCreateTime(cat.getCreateTime());
        return vo;
    }

    /**
     * BOSS 满血 = 100 × 等级^1.3
     */
    public static int bossMaxHp(int bossLevel) {
        return (int) Math.round(GameConstants.BOSS_HP_BASE * Math.pow(bossLevel, GameConstants.BOSS_HP_EXPONENT));
    }

    /**
     * 升级所需经验 = 50 × 等级^1.5
     */
    public static int expToNextLevel(int level) {
        return (int) Math.round(GameConstants.LEVEL_UP_EXP_BASE * Math.pow(level, GameConstants.LEVEL_UP_EXP_EXPONENT));
    }
}
