# 死斗大厅接口设计文档

- 日期：2026-09-16
- 状态：已实现并冒烟通过（2026-09-16 增补名称/类型/状态筛选）
- 关联：2026-09-11-duel-and-focus-design.md（死斗模式）、2026-09-16-applicant-visibility-fix.md

## 增补：查询筛选（2026-09-16 第二轮）

`GET /duel/hall` 增加三个**可选**筛选参数（`DuelHallQueryRequest extends PageRequest`，缺省行为与初版完全一致）：

| 参数 | 说明 | 校验 |
| --- | --- | --- |
| `duelName` | 名称模糊匹配（LIKE，自动 trim） | ≤30 字 |
| `joinMode` | 类型：0 自由加入 / 1 审批加入；空=不限 | @Min(0) @Max(1) → 40000「类型不合法」 |
| `status` | 状态：0 招募中 / 1 进行中；空=不限 | @Min(0) @Max(1) → 40000 |

实现：条件全部走 `LambdaQueryWrapper` 的条件式拼接（`eq(condition, ...)` / `like(condition, ...)`）；控制器参数加 `@Valid` 触发校验（非 @RequestBody 的 POJO 参数校验必须显式 @Valid，首轮冒烟漏加导致非法值直穿，已补）。

冒烟：无条件回归 total=10 不变 ✓；名称「审批制」模糊命中 3 场 ✓；joinMode=1 全为审批制 ✓；status=1 全为进行中 ✓；组合 status=0&joinMode=1 精确 ✓；非法 joinMode=2/status=9 → 40000 ✓

## 一、需求

此前死斗的发现途径只有邀请码海报 / 手动输码 / 已知 id 直查，陌生人无法浏览正在招募的死斗。新增「死斗大厅」：返回**全量招募中 + 进行中**的死斗分页列表，登录用户可浏览后挑选加入/申请。

## 二、接口

`GET /duel/hall?pageNum=1&pageSize=10`（需登录；pageSize 上限钳制 50）

返回 `BaseResponse<Page<DuelHallVO>>`（records/total/current/size 标准分页结构）：

- 排序：招募中（status=0）优先，页内按 id 倒序（最新在前）
- `DuelHallVO` 轻量脱敏版，**不带成员明细**（与大列表规模匹配）：id、duelName、duelDesc、leaderId/leaderName、joinMode、depositPerMember、totalDays、memberCount、status（仅 0/1）、startDate/endDate
- `myRelation`（观看者私有，实时批量查、不进缓存）：`leader` / `member` / `applicant`（有待审申请）/ `null`（陌生人）——前端据此渲染「我的局 / 申请中 / 可加入 / 可申请」按钮态

## 三、实现要点

- `DuelService.hall(pageNum, pageSize, viewerId)`：MP `Page` 分页（复用已配置的 `PaginationInnerInterceptor`）查 `status IN (0,1)`；一页内 3 次批量查询装配（组长昵称 `selectBatchIds`、我的成员行集合、我的待审申请集合），无 N+1
- `myRelation` 判定优先级：leader > member > applicant > null；成员判定沿用 `.ne(status, QUIT)` 口径（已退出视为陌生人，可重新加入）
- 不做 `ensureProgressed` 惰性推进（列表读保持轻量，到点开赛/结算由每日定时任务 + 详情访问兜底，hall 上状态 stale 窗口 ≤24h）

## 四、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 默认分页：total=10，招募中（17,16,15,14）在前、进行中（13..8）在后，页内 id 倒序 | ✓ |
| 2 | 关系标识：申请人 B 见 17 号局 applicant、成员局 member；组长 A 见自己局 leader；陌生人局 null | ✓ |
| 3 | 分页：pageSize=2 → size=2/current=1/total 正确 | ✓ |
| 4 | 上限：pageSize=100 → size 钳制为 50 | ✓ |

## 五、明确不做

按押金/人数/状态筛选排序参数（后续按前端需要加）、未登录浏览（保持登录口径与 /duel/list 一致）、已结算/已解散历史局入厅
