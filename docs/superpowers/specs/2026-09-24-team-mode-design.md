# 组队打卡模式（duel.mode 双玩法）设计文档

- 日期：2026-09-24
- 状态：已实现并冒烟通过
- 关联：2026-09-11-duel-and-focus-design.md（死斗框架）、2026-09-15-admin-leader-economy-design.md（每日即退）、2026-09-20-duel-restrictions-design.md

## 一、需求与口径定版

死斗只是组队玩法的一种。新增**普通组队玩法「组队打卡」**：与死斗同一组队框架，**减去押金机制与退回奖励**，其余（组长、凭证审核、邀请、弹劾、让渡、人数上限、隐藏、任务清单、大厅）全部继承。

四项拷问定版：

| 决策点 | 结论 |
| --- | --- |
| 架构 | **复用** `duel`/`duel_member`/影子计划，新增 `duel.mode`（0=押金死斗〔存量默认〕/1=组队打卡）；不启用 team/group_plan_template 预留表（其语义是"模板"非"组队局"，继续闲置） |
| 打卡方式 | **保留凭证审核**（照片 + 组长审核 = 完成，事件结算照常触发），只减去钱 |
| 进行中进出 | **组队打卡进行中可自由加入/退出**（无押金锁定理由）：加入当天即第 1 天，影子计划 `targetDays` = 剩余天数（endDate−today+1）；退出/被移除影子计划下架，可重进；审批制组队局进行中也可申请/批准 |
| 命名与展示 | 玩法名「组队打卡」；创建页玩法 segment（组队模式隐藏押金输入）；大厅玩法标签 + 独立玩法筛选行 |

资金旁路清单（六触点全部按 mode 处理）：

| 触点 | 押金死斗 | 组队打卡 |
| --- | --- | --- |
| 加入/审批通过扣押金 | 扣 | 跳过 |
| 退出退款 | 仅招募期退 | 任意阶段无退款动作 |
| 移除成员 | 招募全额退/进行中按比例退+罚没池 | 直接移除 |
| 每日即退（凭证通过当天） | floor(押金/T) | `dailyRefund` 自带 `daily<=0` 守卫，天然跳过、零流水 |
| 到期结算 | end_refund 补齐 + 奖池 splitByDays | 仅「待审凭证自动通过 + 成员 checkin_days 落定 + 状态置已结算」 |

## 二、数据模型

```sql
alter table duel add column mode tinyint not null default 0;  -- 存量局自动为押金死斗
```

## 三、实现要点

- `DuelConstant.DUEL_MODE_DEPOSIT=0 / DUEL_MODE_TEAM=1`；`isTeamMode(duel)` 小工具
- 创建：mode=0 校验押金必填（40000「押金死斗需要设置每人押金」），mode=1 落库存 0；`DuelCreateRequest.depositPerMember` 去掉 @NotNull（范围注解保留）
- 进出：join/joinDirect/apply/approveOneApplication 的"仅招募期"校验放宽为 `招募中 || (进行中 && 组队模式)`；quit 同理（组队模式进行中退出无退款动作）
- 余额预检（喵币不足自动拒绝）仅押金死斗执行
- 影子计划天数：进行中加入按剩余天数（`endDate - today + 1`，下限 1）
- 结算 `doSettle`：组队分支在 members 装载后提前返回（autoApprovePending + checkin_days 落定 + memberSettled），跳过全部退款/奖池
- `DuelVO.mode`、`DuelHallVO.mode`、`DuelHallQueryRequest.mode`（大厅玩法筛选）均已暴露

## 四、冒烟结果（组队局=30，双用户 L/B/P）

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 押金局回归：mode=0 不带押金 → 40000「押金死斗需要设置每人押金」 | ✓ |
| 2 | 建组队局（无押金）：mode=1、deposit=0、pool=0，创建者零扣款 | ✓ |
| 3 | 招募期加入：零扣款（对比加押金局的 -100） | ✓ |
| 4 | 进行中：上传凭证 → 组长通过 → 事件结算照常、**无 type=5 每日即退流水**（余额与最新流水均不变） | ✓ |
| 5 | 进行中加入：P 加入成功零扣款，影子计划 targetDays=剩余天数（回填 start/end 错位一天故为 8，公式无误） | ✓ |
| 6 | 进行中移除：直接移除、零退款、被移除通知照发、count-1 | ✓ |
| 7 | 到期结算（end_date < today 惰性触发）：status=2/settled=1/pool=0，退款类流水零新增，成员落定 SETTLED | ✓ |
| 8 | 大厅 mode 筛选：mode=1 命中组队局、mode=0 排除 | ✓ |

## 五、明确不做

mode 创建后修改（押金局↔组队局互转）、组队局进行中修改人数上限/隐藏、启用 team/group_plan_template（模板玩法另行规划）
