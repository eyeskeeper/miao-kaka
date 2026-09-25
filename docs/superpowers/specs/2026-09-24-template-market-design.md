# 计划模板市场（一键套用 + AI 草稿存模板）设计文档

- 日期：2026-09-24
- 状态：已实现并冒烟通过
- 关联：plan create 链路、AI 草稿（/ai/plan/draft）

## 一、口径定版（拷问结论）

- **新建 `plan_template` 表**（group_plan_template/team 继续闲置不删——其 team 语义与本需求不符）
- 创建者= admin → 自动官方模板（is_official=1）；普通用户创建进入公开市场
- 市场：官方置顶 → 使用量 → 最新；每页 20 条
- 一键套用：`POST /plan/from-template` 模板字段映射为既有创建计划链路（任务清单/天数/类型复制），use_count +1
- AI 草稿存模板：无新 AI 端点，前端把草稿字段 POST `/template` 即可
- 删除：仅创建者或 admin（逻辑删除 @TableLogic）

## 二、接口（TemplateController，/template + PlanController）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/template/market?current=` | 市场分页（TemplateVO：名称/描述/类型/天数/任务/官方标/使用量/创建者昵称） |
| POST | `/template` | 创建模板（body：templateName 128/templateDesc 512/planType 0-3/targetDays 1-3650/dailyTasks ≤5） |
| DELETE | `/template/{id}` | 仅创建者或 admin |
| POST | `/plan/from-template` | body `{templateId}` → 一键建计划（返回 PlanVO） |

## 三、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | admin 创建 → is_official=1、市场置顶 | ✓（SQL 提权后需清登录缓存再验——Redis 登录态 5 分钟 TTL，测试时序问题非缺陷） |
| 2 | 用户创建公开模板 → 市场可见（创建者昵称/任务清单/使用量） | ✓ |
| 3 | 一键套用：新计划复制名称/类型/天数/任务清单，use_count+1 | ✓ |
| 4 | 删除权限：非创建者非 admin 删 → 40000「仅创建者或管理员可删除模板」；创建者自删 → ok 且市场移除 | ✓ |

## 四、明确不做

模板编辑与版本（可删重发）、AI 直接发布官方模板、模板收藏、group_plan_template/team 删除（继续闲置）
