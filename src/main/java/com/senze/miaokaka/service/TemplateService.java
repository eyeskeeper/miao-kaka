package com.senze.miaokaka.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.senze.miaokaka.model.dto.template.TemplateCreateRequest;
import com.senze.miaokaka.model.entity.PlanTemplate;
import com.senze.miaokaka.model.vo.PlanVO;
import com.senze.miaokaka.model.vo.TemplateVO;

import java.util.List;

/**
 * 计划模板服务（市场：官方置顶 + 全部公开；一键套用建计划；AI 草稿可存为模板）
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
public interface TemplateService extends IService<PlanTemplate> {

    /**
     * 模板市场（官方置顶 → 使用量 → 最新；每页 20 条）
     */
    Page<TemplateVO> market(long current, Long viewerId);

    /**
     * 创建模板：creator 为 admin 自动官方；返回模板 id
     */
    Long create(Long userId, boolean isAdmin, TemplateCreateRequest request);

    /**
     * 一键套用模板建计划（任务清单复制到新计划），use_count +1
     *
     * @return 新计划
     */
    PlanVO applyToPlan(Long userId, Long templateId);

    /**
     * 删除模板（仅创建者或 admin）
     */
    void remove(Long userId, boolean isAdmin, Long templateId);
}
