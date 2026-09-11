# 喵卡卡 · 死斗模式与习惯养成 设计文档

日期：2026-09-11
状态：已评审（对话式设计共识 + 用户逐节确认）
前置：一期已上线代码（用户/JWT、计划/猫、打卡事件引擎、补卡、AI 草稿/鼓励语、排行、管理端）

## 1. 背景与目标

在一期个人打卡基础上新增两种打卡形式：

1. **10 分钟习惯养成**：降低畏难情绪的轻量模式。用户点击后倒计时 10 分钟，结束即完成打卡。
2. **习惯死斗模式**：多人组队押金对抗。成员充值（一期为虚拟"喵币"）托管，按出勤比例返还，被没收部分进奖池奖励坚持者；打卡需上传照片凭证，经审核后才算有效。

## 2. 范围与决策记录

| 决策点 | 结论 |
|---|---|
| 资金形态 | **方案 A：虚拟押金**——独立"喵币"账户（注册赠 1000），账本与真实货币同构（整数币），押金网关抽象，二期可替换为微信支付；规避平台归集资金的"二清"合规红线 |
| 返还公式 | **方案一：自退 + 奖池再分配**——`返还 = 押金 × 本人确认天数 / T`；没收部分逐笔按其余成员确认天数占比再分配，无人可分则沉没；平台不碰没收款 |
| 豁免 | 无豁免，缺卡当日占比直接没收 |
| 凭证审核 | **AI 预审 + 组长终审**——智谱 GLM-4V 给建议结论与理由（配置开关，未配 key 自动退化为纯组长人工审核） |
| 玩法参数 | 押金 100~5000 喵币（全员统一，创建者定）；天数预设 7/14/21/30 + 自定义 3~365；人数 2~50；创建时定开始日（默认次日），开始前可加入/退出（全额退），开始后锁定 |
| 10 分钟模式归属 | **后端不可知**——前端本地倒计时 + 现有 `POST /check_in`；后端仅保留 `plan_mode` 标记列（无任何逻辑），作弊无收益故不做服务端强制 |
| 死斗接入引擎 | **影子计划**——成员加入即自动创建关联 `duel_id` 的个人计划，猫养成/随机事件/连击/防重唯一键/日历全部复用一期引擎 |
| 照片存储 | 本地磁盘卷 + `StorageService` 接口抽象（`${app.upload-dir}`，`/uploads/**` 静态映射），二期可换 OSS |

范围拆解（实现顺序即依赖顺序）：钱包账本 → 死斗基础（影子计划）→ 图片上传 → 死斗打卡+审核 → 结算引擎。10 分钟模式不在后端实现范围内。

## 3. 数据模型

### 3.1 现有表加列（3 处）

| 表 | 加列 | 说明 |
|---|---|---|
| `user` | `miao_coins int default 1000` | 喵币余额（押金货币），注册即赠 1000 |
| `check_in_plan` | `plan_mode tinyint default 0` | 0 普通 / 1 十分钟习惯（纯标记，无逻辑） |
| `check_in_plan` | `duel_id bigint null` | 影子计划关联的死斗 id |
| `check_in_plan` | `plan_source` 枚举扩展 | 增加 `2:死斗挑战` |
| `check_in_record` | `status` 枚举扩展 | 增加 `3:待审核`（通过→0，驳回→2） |

### 3.2 新表（4 张）

**`coin_transaction` 喵币流水**

- `id`、`user_id`、`type`（0 注册赠送 / 1 押金支出 / 2 退还 / 3 奖池分得 / 4 管理员调整）、`amount`（正收入负支出）、`balance_after`（变动后余额，对账用）、`biz_id`（关联 duel）、`remark`、`create_time`
- 索引：`idx_user(user_id)`、`idx_biz(biz_id)`
- 原则：余额只随流水变动，每笔写 `balance_after`，任何时点可重放对账

**`duel` 死斗挑战**

- `id`、`duel_name`、`duel_desc`、`leader_id`（创建者即组长）、`deposit_per_member`（100~5000）、`total_days`（3~365）、`start_date`（默认次日）、`end_date`（冗余 = start_date + total_days − 1）、`status`（0 招募中 / 1 进行中 / 2 已结算 / 3 已解散）、`member_count`（冗余）、`settled tinyint`（结算幂等标记）、审计字段
- 索引：`idx_leader`、`idx_status`、`idx_start_date`

**`duel_member` 成员**

- `id`、`duel_id`、`user_id`、`plan_id`（影子计划）、`deposit`（本人押金快照）、`checkin_days`（冗余，结算时重算）、`status`（0 已加入 / 1 进行中 / 2 已结算 / 3 已退出）、`join_time`
- 唯一键 `uk(duel_id, user_id)`；索引 `idx_user`、`idx_plan`

**`check_in_evidence` 凭证与审核**

- `id`、`record_id`（唯一，对应 check_in_record）、`duel_id`、`user_id`、`image_path`（本地相对路径）、`review_status`（0 待审 / 1 通过 / 2 驳回）、`reviewer_id`、`is_self_review`（组长自审公示标记）、`ai_suggestion`（空=未启用 / 0 建议通过 / 1 建议驳回）、`ai_reason`、`review_remark`（驳回必填）、`review_time`、`create_time`

### 3.3 影子计划

成员加入死斗时自动创建：`check_in_plan(plan_source=2, duel_id, plan_name=duel_name, plan_type=3 其他, target_days=total_days, status=0)`，并按一期规则自动生成猫。影子计划正常出现在"我的计划列表"，死斗打卡同样养猫——这是复用引擎的自然结果，也是特性。

## 4. 资金账本与结算

- **加入即扣押金**（type=1），不引入冻结态；事务内 `update user set miao_coins = miao_coins - ?` 行级锁天然防并发超扣，余额不足拒绝加入
- 创建者创建即成为第一名成员并扣押金
- 开始前退出：全额退（type=2），成员标记已退出
- **结算公式**（T = total_days，确认天数 = 影子计划在 [start_date, end_date] 内 status=0 的记录数）：
  - `返还_i = deposit_i × days_i / T`（向下取整，余数留在没收侧）
  - `没收_i = deposit_i − 返还_i`
  - 没收逐笔进入奖池，按"其余成员确认天数占比"分配（type=3）；某笔没收面对的全员天数皆为 0 时沉没（流水 remark 记录）
- 结算幂等：`duel.settled` + 事务；每日定时任务扫描 `end_date < today 且 status=1` 的挑战结算，死斗详情接口 lazy 兜底
- 宽容条款：**结算时任然待审核的记录自动视为通过**——组长不作为不坑成员的钱；驳回只能明示

## 5. 死斗流程

1. **创建**：`POST /duel`（名称/押金/天数/开始日）→ 扣创建者押金 + 建影子计划 + 建 duel/duel_member
2. **加入**：`POST /duel/{id}/join` → 招募中、未满 50 人、未重复加入 → 扣押金 + 影子计划
3. **退出**：`POST /duel/{id}/quit` → 仅招募中，全额退
4. **开赛**：定时任务把 `start_date ≤ today 且 status=0` 置为进行中并锁定成员，无手动开赛接口
5. **打卡**：`POST /duel/{id}/check_in`（multipart：照片 ≤5MB jpg/png/webp + 可选备注）→ 校验成员/进行中/当日无任何状态记录（含待审核）→ 插 `check_in_record(status=3)` + 凭证 → AI 开关开启时同步调 GLM-4V 写建议（短超时，失败留空不阻塞）
6. **审核**（仅组长）：待审列表 + 通过/驳回。通过 → record=0，触发影子计划结算：重算连击 + 触发一次猫事件 + 经验/积分（把一期引擎的"记录插入"与"事件结算"拆分共用）；驳回 → record=2 + 理由，无任何结算
7. **组长本人打卡**：AI 开启时由 AI 结论直接生效（无人可审，详情页公示"AI 审核"）；AI 未开启时组长自审（`is_self_review=1` 公示）
8. **详情**：`GET /duel/{id}` 成员（昵称/头像/天数/进度）、奖池总额、我的押金状态、组长可见待审数

## 6. 照片存储与 AI 预审

- `StorageService` 接口 + `LocalStorageService`：存 `${app.upload-dir}`（默认 `/data/uploads`），日期分目录 + UUID 文件名；`/uploads/**` 静态资源映射；二期换 OSS 仅替换实现类
- 智谱 GLM-4V：Spring AI zhipuai starter，独立 ChatClient；`app.ai.vision.enabled=true` 且已配 key 才装配（`@ConditionalOnProperty` + `ObjectProvider` 注入，未装配时业务代码自然降级）
- 预审提示词：判断图片是否为合理的日常习惯打卡凭证，仅输出 JSON `{"verdict":"pass|reject","reason":"..."}`；解析失败视为未给出建议

## 7. 10 分钟习惯养成（后端不可知）

- 前端本地倒计时 10 分钟，结束调现有 `POST /check_in`；中途退出无任何状态与惩罚（主旨即降低畏难）
- 后端唯一痕迹：`plan_mode` 标记列，供客户端筛选与未来统计，不参与任何校验
- 作弊分析：跳过计时所得与普通打卡完全相同，现有 `uk_user_plan_date` 唯一键已兜住唯一真实威胁（当日重复打卡），故不做服务端强制

## 8. 接口清单（10 个新端点）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/duel` | 创建死斗（创建者即第一成员，扣押金） |
| GET | `/duel/list` | 我创建的 + 我参与的 |
| GET | `/duel/{id}` | 详情（成员/天数/奖池/我的状态） |
| POST | `/duel/{id}/join` | 加入（招募中，扣押金） |
| POST | `/duel/{id}/quit` | 退出（仅招募中，全额退） |
| POST | `/duel/{id}/check_in` | 打卡（multipart 照片 + 备注） |
| GET | `/duel/{id}/review/pending` | 组长待审列表 |
| POST | `/duel/{id}/review` | 审核（通过/驳回 + 理由） |
| POST | `/upload/image` | 通用图片上传 |
| GET | `/wallet` | 喵币余额 + 流水分页 |

## 9. 边界情况

- **影子计划通道封闭**（防绕过凭证体系）：`plan_source=2` 的影子计划禁止走普通 `POST /check_in`、`POST /check_in/makeup`，禁止删除/暂停（`POST /plan/delete`、`/plan/update` 状态切换）——死斗打卡只能经 `/duel/{id}/check_in`，生命周期完全由死斗流程管理
- 重复加入、押金不足、满员（>50）、非招募期加入/退出 → 业务异常拒绝
- 当日已存在任意状态记录（含待审核）→ 拒绝重复打卡（影子计划唯一键 + 代码前置校验）
- 审核通过才计天数；驳回记录留痕不计；待审核跨到结算日自动通过
- 结算余数处理：返还向下取整，余数留在没收侧进奖池
- 结算面对"其余成员天数全 0"：该笔没收沉没，流水备注
- 并发扣押金：同一 user 行 UPDATE 串行化 + 事务
- 挑战期间无转让组长、无修改押金/天数（YAGNI，二期）

## 10. 实现顺序

1. 建表 SQL（4 新表 + 加列）并应用本地库
2. pom：`spring-ai-starter-model-zhipuai`
3. 钱包：实体/Mapper/Service（扣款/入账/流水，行锁 + `balance_after`）
4. 死斗基础：创建/加入/退出/详情/列表 + 影子计划生成 + 开赛/结算定时任务骨架
5. 图片上传：`StorageService` + 本地实现 + 静态映射 + `POST /upload/image`
6. 死斗打卡 + 凭证 + 审核流（含 GLM-4V 预审开关）
7. 结算引擎（公式纯函数 + 幂等 + 奖池分配）
8. Swagger/curl 全链路冒烟（含结算数值断言）

## 11. 测试策略

- **结算公式单测**：分配逻辑实现为纯函数（输入成员押金/天数/总天数，输出各人返还与奖池分配），覆盖全员满勤、部分缺勤、全零沉没、余数取整四类用例
- **全链路冒烟**：注册（赠币）→ 创建/加入 → 开赛 → 带图打卡 → AI/人工审核 → 通过后猫事件结算 → 到期结算数值断言（逐人核对钱包余额与流水 `balance_after` 连续性）
- 边界用例走 Swagger 手工覆盖（重复加入/超额加入/开始后退出/重复打卡）
