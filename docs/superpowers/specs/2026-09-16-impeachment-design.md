# 死斗弹劾组长 设计文档

- 日期：2026-09-16
- 状态：已实现并冒烟通过
- 关联：2026-09-15-admin-leader-economy-design.md（组长能力与每日即退经济模型）

## 一、需求

小组成员可以弹劾组长：

1. 成员 A 发起弹劾，附带原因（20 字内）
2. 前端小组页面出现「弹劾」「维持」按钮
3. 其他成员选择其一；发起人默认是「弹劾」
4. 弹劾数大于小组人数的一半 → 弹劾成功
5. 成功后发起人成为新组长，老组长变为普通成员

## 二、口径定版（四问四答）

| 决策点 | 结论 |
| --- | --- |
| 未过半如何结束 | 发起后 24 小时到期自动判负；定时任务（每 10 分钟）扫期 + 详情访问/投票时惰性兜底 |
| 票数基数 | 当前正式成员数（含组长与发起人，不含已退出/已移除，即 `duel.member_count`）；弹劾票**严格大于**一半才成功；未投票视为支持组长 |
| 适用阶段 | 仅进行中（RUNNING）；招募中用退出 + 让渡已足够 |
| 失败后再发起 | 判负后 `finish_time + 24h` 冷却，冷却期内该死斗不能再发起；成功不冷却（可立即弹劾新组长） |

补充细则（实现时钉死）：

- 组长不能发起弹劾，也不能给自己投弹劾票（投维持不受限）
- 一票定死不可改票：`uk_impeach_user (impeachment_id, user_id)` 数据库唯一键兜底
- 同一时刻仅允许一个进行中弹劾
- 维持票数学上过半（维持票数 × 2 > 成员数）时提前判负，不让死票挂满 24 小时
- 弹劾进行中，组长**不能移除发起人**（防组长移人掐死弹劾）
- 组长让渡成功时，进行中的弹劾自动判负（fail_reason=组长已变更，弹劾终止）——弹劾对象已换人
- 成功瞬间复核发起人仍是正式成员（极端并发兜底，否则按失败结算）
- 角色由 `duel.leader_id` 派生（成员表无角色列）：换组长即生效，老组长自动降为普通成员，无需改成员行
- **全程不涉及任何资金变动**（弹劾是治理行为，不是经济行为）

## 三、数据模型

```sql
duel_impeachment        -- 弹劾主表
  id, duel_id, initiator_id, reason varchar(20),
  status tinyint (0进行中/1成功/2失败),
  fail_reason varchar(64),      -- 维持票过半 / 投票截止，未过半 / 组长已变更，弹劾终止 / 发起人已离局
  impeach_cnt int, maintain_cnt int,   -- 原子计数列（投票时 setSql cnt=cnt+1，免 count 查询）
  expire_time datetime,          -- 发起 + 24h
  finish_time datetime, create_time
  index idx_duel_status (duel_id, status)

duel_impeachment_vote   -- 投票表（一人一票）
  id, impeachment_id, duel_id, user_id, vote tinyint (0维持/1弹劾), create_time
  unique key uk_impeach_user (impeachment_id, user_id)
```

## 四、接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/duel/{duelId}/impeach` | 发起弹劾，body `{reason}`（≤20 字必填） |
| POST | `/duel/{duelId}/impeachment/vote` | 投票，body `{impeachmentId, vote}`（0 维持/1 弹劾） |
| GET | `/duel/{duelId}`（扩展） | `DuelVO` 新增 `impeachment` 概要 + `myImpeachVote` |

`DuelVO.impeachment`（观看者无关，进 `DuelAggData` 60s 聚合缓存 + 写时逐出）：
`{id, initiatorId, initiatorName, reason, impeachCount, maintainCount, totalCount, expireTime}`。

`myImpeachVote`（null 未投/0 维持/1 弹劾）为个性化字段，每次实时查，绝不进缓存——与 myRole/myPlanId 同一纪律。已结束的弹劾不再展示（留表审计）。

## 五、关键实现

- `DuelImpeachmentService(Impl)`：initiate / vote / resolveExpired（扫期）/ assembleActiveVO（含过期惰性判负）/ getActiveInitiator / terminateOnLeaderChange / myVoteOf。只依赖 Mapper 层（Duel/DuelMember/User/Vote），不依赖 DuelService，避免循环依赖
- **成功结算（事务内）**：插票（撞唯一键 →「你已投过票」）→ `setSql` 原子累加计数列 → 重读计数 → 过半则条件更新 `status 0→1` 抢占成功资格（沿用 settled 幂等闸门，仅 rows=1 者执行换组长）→ `duel.leader_id = initiator_id`
- **过期结算**：条件更新 `0→2` 判负 + 逐出聚合缓存；`@Scheduled(cron = "0 */10 * * * ?")` 扫期；详情装载（缓存 miss）与投票入口双重惰性兜底
- `DuelServiceImpl.transfer` 让渡成功后调 `terminateOnLeaderChange`；`removeMember` 加发起人保护
- 注意：聚合缓存 60s TTL 内详情可能短暂展示已过期弹劾（前端可按 expireTime 渲染倒计时归零）；数据最迟 10 分钟内由扫期自愈

## 六、冒烟结果（3 人局，L/A/B，N 为局外人，押金 100）

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 招募中发起 → 拒「仅进行中的死斗可以发起弹劾」 | ✓ |
| 2 | 组长发起 → 拒；局外人发起 → 拒「仅正式成员」 | ✓ |
| 3 | A 发起成功：impeach_cnt=1、expire=+24h、VO 概要+A 的 myImpeachVote=1 | ✓ |
| 4 | B 重复发起 → 拒「已有进行中的弹劾」 | ✓ |
| 5 | B 投弹劾票 → 2×2>3 即成功：leaderId 82→83、isLeader 互换、老组长 myRole=member、status=1 | ✓ |
| 6 | 重复投票 → 撞唯一键拒「你已投过票，一票定死不可改」 | ✓ |
| 7 | 组长给自己投弹劾票 → 拒 | ✓ |
| 8 | 新组长被弹劾：维持票 1→2 过半 → 提前判负「维持票过半」，组长不变 | ✓ |
| 9 | 判负后立即再发起 → 拒「冷却 24 小时」；SQL 拨 finish_time -25h 后发起成功 | ✓ |
| 10 | 成功后不冷却：imp#1 成功后 L 立即发起 imp#2 直接成功 | ✓ |
| 11 | 到期未过半：详情访问惰性判负 status=2「投票截止，未过半」；截止后投票 → 拒「投票已截止」 | ✓ |
| 12 | 让渡组长 → 进行中弹劾自动判负「组长已变更，弹劾终止」 | ✓ |
| 13 | 弹劾进行中移除发起人 → 拒「弹劾进行中，不能移除弹劾发起人」 | ✓ |
| 14 | 资金零断言：balance_sum=3700 / tx_count=62 全程不变；member_count=3、total_pool=300、removed_pool=0 不变 | ✓ |

## 七、明确不做

弹劾历史列表与逐票明细公开（仅聚合计数）、投票提醒推送（前端轮询详情即可）、弹劾成功二次确认、发起人申诉
