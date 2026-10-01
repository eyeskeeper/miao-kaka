# 猫猫战斗系统重构（随机事件 / BOSS 配额 / 组队共享 BOSS）设计文档

- 日期：2026-09-25
- 状态：已实现并冒烟通过
- 关联：一期事件引擎（攻击/属性/暴击）、组队打卡（duel.mode=1）

## 一、口径定版（拷问结论）

1. **事件结构（互斥制）**：一次打卡一个主事件——70% 攻击 BOSS（内含 1/7 暴击 ×2）+ 30% 随机事件。随机事件四分支：获得物品 40% / 属性提升 25% / 属性降低 25% / 发现饰品 10%。随机事件日不推进 BOSS 血量
2. **BOSS 配额制**：配额 = ceil(targetDays/7)；全部配额击杀后再打卡 → 只获得喵币（个人 20/次、组队 40/次 = ×2，归打卡者）
3. **80% 冗余**：总血量按 80% 出勤可全清设计；100% 出勤提前 ~4 天清完，击败最后一只配额 BOSS 当天一次性「讨伐完成礼」100 喵币（个人给打卡者；组队全队每人）——即 100% 用户的额外奖励
4. **组队共享 BOSS**：组队局 BOSS 为全队共享实体（挂 duel），HP = 单只基准 × 当前成员数（进出动态重算等比例缩放）；任何成员打卡推进同一只；**击败奖励全局共享**（全队每人经验+积分）；喵币×2 归打卡者
5. **接口预留**：CheckInResultVO 加 eventCode/picCode（像素图占位= eventCode）/accessoryCode；饰品本期只「发现」

## 二、BOSS 数值（80% 冗余模拟）

`HP(n) = 60 + (n-1) × 20`（BOSS_HP_FIRST=60 / BOSS_HP_STEP=20，GameConstants 可调）；配额 = ceil(targetDays/7)

| BOSS | HP | 80% 出勤（17 日，日均 14） | 100% 出勤（21 日） |
| --- | --- | --- | --- |
| 1 | 60 | 第 6 天 | 第 5 天 |
| 2 | 80 | 第 13 天 | 第 11 天 |
| 3 | 100 | **第 20 天全清** | 第 17 天全清，提前 4 天 |

30 天 5 只（总 400）：80% 出勤 24 日×14≈336 ✓；100% 提前 7 天进喵币期

## 三、数据模型

- `duel` 加列：boss_level / boss_name / boss_hp / boss_max_hp / boss_killed / boss_quota(=ceil(totalDays/7))——组队共享 BOSS 状态
- `check_in_plan` 加列：boss_quota(=ceil(targetDays/7)) / boss_killed——个人局配额追踪
- 随机事件物品复用 `user_item` 背包；无新表

## 四、实现要点

- 事件掷骰重构：roll ≤30 随机事件四分支（物品=随机补卡券/双倍经验卡入背包；属性+复用 stat；属性−有下限保护不低于 5；饰品仅文案）；roll >30 攻击分支
- 攻击分流：影子计划（duel_id 非空）→ `resolveSharedBossAttack` 攻击 duel 上的共享 BOSS；个人计划 → `resolvePersonalBossEvent` 攻击猫的 BOSS（新曲线 HP=60+20×(n-1)）
- 全清判定与奖励：`boss_killed >= boss_quota` → 喵币期（个人 20 / 组队 40，walletService.grantCheckInCoins 新方法，流水 COIN_TX_CHECKIN=7）；达成全清的当次击败额外 100 喵币讨伐礼
- 共享 BOSS 持久化：伤害与刷新即时 `duelMapper.updateById` + 逐出聚合缓存
- **共享 BOSS 随成员数重算** `recalcSharedBoss`：加入/退出/移除后等比例缩放（curHp × 新max/旧max）
- 随机事件物品：`MallService.addItem`（公开发放接口，内部复用 addItemInternal upsert）
- 组队共享 BOSS 击败的全员奖励不含喵币（喵币只进打卡者与讨伐礼），避免钱包重复记账

## 五、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 随机事件四分支均触发（ITEM/STAT±/ACCESSORY），属性+累计生效（attack 10→14） | ✓ |
| 2 | 属性−下限保护（不低于 5） | ✓ |
| 3 | 物品入背包（双倍经验卡数量变化） | ✓ |
| 4 | 饰品仅文案+eventCode=ACCESSORY | ✓ |
| 5 | 组队共享 BOSS：交替打卡扣同一 HP 且持久化（180→160→152→147）、随机事件日不扣 | ✓ |
| 6 | 组队击败：全员经验/积分、BOSS 升级刷新、喵币×2 归打卡者 | ✓ |
| 7 | 成员加入 HP ×成员数放大（60→180） | ✓ |
| 8 | 配额制：createPlan 初始化 quota=ceil(targetDays/7)（修复了未初始化导致的"第 1 杀即全清"缺陷） | ✓ |

## 六、明确不做

饰品装备与生效、事件图片素材、物品交易、属性重置道具、概率动态调整、组队 BOSS 等级随人数成长、存量 BOSS 血量回填
