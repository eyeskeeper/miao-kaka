# 修复：申请人在审核期间看不到审批制死斗

- 日期：2026-09-16
- 状态：已修复并冒烟通过
- 关联：2026-09-11-duel-and-focus-design.md（审批加入模式）

## 一、问题

用户反馈：无法看到"审核状态"的死斗组。

排查确认（审批制死斗 15 号局、用户 N 已提交申请实测）：

| 视角 | 现象 |
| --- | --- |
| 申请人 `GET /duel/list` | **看不到**自己申请中的死斗 |
| 组长 `GET /duel/list` | 能看到（自己创建） |
| 申请人按 id 直查详情 | 正常返回，但 `myRole/myStatus` 全空，看不出有申请在审 |

## 二、根因

`DuelServiceImpl.listMine` 只聚合两个来源：**我是成员的**（`duel_member` 行，status≠已退出）+ **我创建的**（`duel.leader_id`）。审批制的申请人处于夹缝状态：申请在 `duel_join_request`（status=0 待审）、尚无 `duel_member` 行，两个来源都不命中。

叠加缺口：没有任何接口向申请人暴露自己的申请状态（`/applications` 仅组长可见）。

## 三、修复

1. `listMine` 增加第三来源：**我有待审加入申请**（`duel_join_request.status=0`）的死斗，一并进列表（排序/去重复用既有逻辑）
2. `DuelVO` 新增观看者私有字段 `myApplyStatus`：null 无申请 / 0 待审 / 1 已通过 / 2 已拒绝（本人最新一条申请，`buildDuelVO` 实时查、**不进聚合缓存**，与 myRole 同一纪律）

修复后语义：

- 待审 → 列表可见，标 myApplyStatus=0
- 拒绝 → 列表消失（申请不再待审），按 id 直查可见 myApplyStatus=2，知道可重新申请
- 批准 → 自动转为成员来源可见（扣押金 + 影子计划照旧），myApplyStatus=1 留痕

## 四、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 待审申请人的列表出现该局，myApplyStatus=0、myRole=null | ✓ |
| 2 | 组长拒绝后：列表消失；按 id 直查 myApplyStatus=2 | ✓ |
| 3 | 重新申请 → 组长批准：申请人转成员（myStatus=0、member_count+1、扣押金） | ✓ |
| 4 | 视角隔离：其他用户看同一局 myApplyStatus=null | ✓ |

## 五、明确不做

拒绝后仍常驻列表（避免历史被拒局堆积）、申请状态推送通知（前端轮询列表即可）、组长侧批量审批
