# 死斗组限制（人数上限/隐藏/一键通过/开始时间）设计文档

- 日期：2026-09-20
- 状态：已实现并冒烟通过
- 关联：2026-09-11-duel-and-focus-design.md（审批加入模式）、2026-09-16-duel-hall-design.md（招募大厅）

## 一、需求与口径定版

1. **人数上限**：创建时组长必须确定 `maxMembers`（**2~50**，含组长）；满员判定从全局硬顶 `MEMBER_MAX` 切换为每局 `duel.max_members`（`int not null default 50`，ALTER 自动回填存量局为 50）
2. **开始时间**：创建时 startDate 语义收紧为**最早为明天**（原来允许当天，现 `!startDate.isAfter(today)` 即 40000「开始日期最早为明天」；不传仍默认明天）
3. **隐藏**：`duel.hidden`（默认 0）。隐藏局**不出现在招募大厅**（hall 查询排除，含名称/类型/状态筛选场景）；仅可通过**死斗组号（id 直查详情）**与**邀请海报/邀请码**发现；自己的 `/duel/list` 照常可见；`DuelVO.hidden` 暴露给前端展示锁标
4. **组员审核**：`joinMode`（0 自由/1 审批）已有，零改动
5. **一键通过**：`POST /duel/{duelId}/applications/approve-all`（仅组长、仅招募中）——按申请 id 升序逐个通过（与单条审核共用同一套通过逻辑：满员预检/在册预检/余额预检/事务内扣押金+入组+留痕）；**喵币不足自动拒绝并留痕**；**通过到 maxMembers 满员即停止，剩余申请保持待审**（局仍是招募中，名额释放后组长可再批）；返回 `{approved, rejected, skipped}`
6. **满员自动开赛：已抛弃**（grill-me Q2/Q3 答复）——满员的局只是不能再进人，状态流转仍按开始日期走既有定时任务 + 惰性推进
7. 附带补口子：`apply`（提交申请）原不检查满员 → 增加拦截「该死斗已满员，无法申请」

## 二、实现要点

- `DuelServiceImpl` 抽取 `approveOneApplication(duel, joinRequest, remark)` 私有方法（返回 SUCCESS/FULL/INSUFFICIENT 三态），单条审核与一键通过共用；单条审核遇 FULL 保持原有 40000 报错语义
- `maxMembersOf(duel)` 小工具（null→50 防御），替换 joinDuelTx/createMembershipTx/reviewApplication 中的 MEMBER_MAX 比较
- 建表块已折入三列（daily_tasks/max_members/hidden），存量库走尾部 ALTER
- `DuelVO` 新增 `hidden`、`maxMembers`（观看者无关，进聚合缓存）

## 三、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 创建校验：缺 maxMembers / =1 / =51 → 40000（「人数上限不能为空/最少 2 人/最多 50 人」） | ✓ |
| 2 | startDate=今天 → 40000「开始日期最早为明天」 | ✓ |
| 3 | maxMembers=2 隐藏局：组长+1 即满 → 第 3 人 join 拒「该死斗已满员（2 人）」；状态仍 0 招募中（不自动开赛） | ✓ |
| 4 | 隐藏局不在 hall（含筛选场景）；按 id 直查 hidden=true、listMine 照常 | ✓ |
| 5 | 一键通过：3 申请者（1 人被 SQL 扣穷）→ approved=2 / rejected=1（留痕「喵币不足（需 100），自动拒绝」）/ skipped=0；member_count=3 | ✓ |
| 6 | skipped 路径：maxMembers=2 局 2 申请者 → approved=1 / skipped=1，剩余待审保留 | ✓ |
| 7 | apply 满员拦截「该死斗已满员，无法申请」；单条审核满员 → 40000「该死斗已满员，无法通过申请」 | ✓ |
| 8 | 对账：通过者各扣押金 100（B=442、P=900），balance_after 链完整 | ✓ |

## 四、明确不做

满员自动开赛（已抛弃）、修改 hidden/maxMembers 的接口、隐藏局的转让/弹劾等流程变更（一切照旧）
