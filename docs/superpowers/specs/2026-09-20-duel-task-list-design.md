# 死斗任务清单（AI 草稿 + 部分打卡 + 凭证门槛）设计文档

- 日期：2026-09-20
- 状态：已实现并冒烟通过
- 关联：2026-09-11-duel-and-focus-design.md（死斗打卡凭证流）、2026-09-11-task-toggle-design.md（任务勾选引擎）

## 一、需求与口径

死斗组此前没有打卡内容（影子计划 daily_tasks 恒为 null）。本次让死斗像普通打卡计划一样：

1. **创建时接入 AI 生成任务草稿**——复用现有 `POST /ai/plan/draft`（组长输入死斗主题描述 → 返回任务清单 → 确认后随 `DuelCreateRequest.dailyTasks` 创建），AI 侧零改动
2. **包含部分打卡功能**——成员对自己的影子计划逐项勾选（toggleTask 对死斗的分支早已存在：勾满不自动打卡，提示"请到死斗入口上传照片凭证完成打卡"）
3. **凭证是唯一完成门槛**——上传凭证**不校验**任务勾选状态；照片 + 审核（通过→事件结算/每日即退）才是真的完成

## 二、数据模型与传播链

```sql
alter table duel add column daily_tasks json default null;  -- 单一来源，组长创建时带入
```

传播链（一处改动全路径生效）：`DuelCreateRequest.dailyTasks`（≤5 项，trim/去空）→ `duel.daily_tasks` → `createMembershipTx` 解析 → `createShadowPlan(..., dailyTasks)` 复制到成员影子计划（daily_tasks + total_tasks=size + task_progress=""）；创建/自由加入/审批通过/退出重进四个路径共用。`DuelVO.dailyTasks` 经聚合缓存暴露给前端。

兼容：不带 dailyTasks 创建的死斗（含全部存量局）行为与从前完全一致——影子计划无任务，勾选报"该计划未配置每日任务，直接打卡即可"；不回填存量局。

## 三、实现清单

- `duel` 表 + `Duel` 实体 + `DuelCreateRequest`（`@Size(max=5)`）
- `CheckInPlanService.createShadowPlan` 签名加 `List<String> dailyTasks`：非空时落 JSON 并 totalTasks=size，空则 totalTasks=1 维持旧行为
- `DuelServiceImpl.create`（hutool JSONUtil 规整序列化）、`createMembershipTx`（解析传入）、`buildDuelVO`（解析暴露）
- **未改动**：toggleTask（死斗分支已有）、凭证上传入口、AI 服务、审核结算链路

## 四、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | AI 草稿回归：死斗主题描述 → 返回 5 项任务（DeepSeek 实调） | ✓ |
| 2 | 带 3 项任务建局 20：duel.daily_tasks 落库、VO 暴露、组长影子计划（plan 67）复制 total=3 | ✓ |
| 3 | 成员 B 加入：影子计划（plan 68）复制同一清单 | ✓ |
| 4 | 部分勾选 completedTasks 1→2→3；勾满提示传凭证、autoChecked=false、无 record 生成 | ✓ |
| 5 | 传凭证进入待审核（record 19 status=3）——不要求任务全勾完 | ✓ |
| 6 | 传凭证后当日勾选冻结：「今日已打卡，任务勾选已冻结」 | ✓ |
| 7 | 存量无任务局（12 号局）：勾选报「该计划未配置每日任务」 | ✓ |
| 8 | 组长审核通过：record→0、影子计划连击 1/1、事件结算（猫/BOSS 字段返回）、每日即退 +14（type=5, balance 642） | ✓ |

（冒烟中两次脚本失误与本次功能无关：Git Bash multipart 路径需 cygpath -w；check_in_record/coin_transaction/check_in_plan 主键列名分别为 record_id/id/id，SQL 手写对账时注意。）

## 五、明确不做

存量局回填任务、死斗专用 AI 提示词、创建后修改任务清单的接口、凭证上传要求任务全勾完
