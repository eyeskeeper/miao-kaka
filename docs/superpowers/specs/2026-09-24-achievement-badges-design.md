# 成就徽章系统（默认 8 枚）设计文档

- 日期：2026-09-24
- 状态：已实现并冒烟通过
- 关联：2026-09-11-task-toggle-design.md（事件结算汇聚点）

## 一、需求与口径

成就徽章体系第一期：默认 8 枚（拷问定版），解锁幂等（一人一徽唯一键）、只增不删（历史不回收）、仅上线后增量解锁（不做全量历史补发）。

## 二、徽章清单

| code | 名称 | 解锁条件 |
| --- | --- | --- |
| FIRST_CHECKIN | 初来乍到 | 完成第一次打卡 |
| STREAK_7 | 七日之约 | 单个计划连续打卡 7 天 |
| STREAK_30 | 月度坚持 | 单个计划连续打卡 30 天 |
| STREAK_100 | 百日长征 | 单个计划连续打卡 100 天 |
| FULL_WEEK | 全勤之星 | 全勤连击达到 7 天 |
| BOSS_1 | 猫武士 | 击败第 1 只 BOSS |
| BOSS_10 | 屠龙者 | 累计击败 10 只 BOSS |
| MAKEUP_FIRST | 不弃不离 | 完成第一次补卡 |

## 三、实现

- 表 `user_achievement`（unique uk_user_code，DuplicateKeyException 兜底并发双解锁）
- `AchievementService`：award（幂等授予）/ evaluateCheckInBadges（打卡结算后评估 7 枚：首打/单计划连击×3/全勤/BOSS×2，条件计数查询）/ evaluateMakeupBadge（首次补卡）/ wall（徽章墙）
- **钩子位置**：两条打卡完成链路的汇聚点 `applyGrowthAndPoints` 尾部（事务内，与打卡原子）+ `doMakeupInTx` 尾部（补卡）
- 结果返回：`CheckInResultVO.unlockedBadges` / `MakeupResultVO.unlockedBadges`（新解锁名称，前端结果弹窗展示"🏅 解锁徽章：xxx"）
- `GET /check_in/achievements`：8 枚清单 + 解锁状态/时间（徽章墙）

## 四、冒烟结果

- 新用户首次打卡 → CheckInResultVO `unlockedBadges=['初来乍到']` ✓
- 徽章墙 8 枚全量返回、解锁状态准确 ✓
- 补卡（用券）→ 解锁「不弃不离」 ✓
- 幂等：重复打卡不重复授予（唯一键 + hasBadge 前置） ✓

## 五、明确不做

全量历史补发、徽章回收、组队系徽章（后端钩子已预留 memberSettled 时刻，二期接入）
