# 喵卡卡 · 一期核心（打卡 + 猫精灵养成）设计文档

日期：2026-09-10（实现于本日；本文为回溯归档——一期早于 specs 目录约定建立，原文散见于 CLAUDE.md 与评审对话，现整合归档）
状态：已实现并通过 17 步全链路冒烟（提交 `067c593`）
作者：设计共识由冉森与 AI 助手经三轮拷问评审达成；原始建表稿（6 张表）由冉森独立设计

## 1. 定位与范围

- **定位**：真实上线的打卡产品后端，以"猫精灵养成"强化打卡动机
- **一期范围**：用户认证（JWT）+ 个人打卡 + 猫精灵养成 + AI 助手 + 连击排行榜 + 最小管理端；纯后端 API，Swagger 调试
- **二期候选（一期明确不做）**：小组/计划模板、微信登录、提醒推送、猫自由对话、装扮商店、BOSS 反击/PVP

## 2. 决策记录（三轮拷问 Q1~Q20 浓缩）

| 决策点 | 结论 |
|---|---|
| 定位 | 真实上线产品（非练手），认证/部署按上线标准做 |
| 客户端 | 一期纯后端 API；微信字段（unionid/mpopenid）预留，微信登录二期 |
| 范围 | 小组与模板表保留但代码二期 |
| 猫的归属 | **多猫方案**：每个打卡计划养一只猫（`cat_spirit.uk_plan_id`）——用户权衡后否决了"每用户一猫"的情感投射方案，选择收集玩法 |
| 打卡→成长 | 打卡 1 次 = 一次随机事件（属性提升 / 攻击 BOSS）；BOSS 血量不自动回复、可多次攻击；断签不惩罚猫，只断连击 |
| 积分经济 | 一期积分唯一花处 = 补卡（打卡赚分 → 断签花分补卡的自然闭环）；商店二期 |
| 补卡 | 50 积分/天，自然月限 2 次，恢复连击但记录标记补卡（连击含金量分层） |
| AI 双点位 | ①对话式生成打卡计划（草稿确认制，AI 只出草稿、创建走统一接口，防幻觉污染库）；②打卡后猫口吻鼓励语（同步生成 + 超时降级本地语录，**打卡主流程永不因 AI 失败而失败**） |
| 认证 | JWT（Hutool HS256）+ BCrypt；仅引 `spring-security-crypto`，不引 Spring Security 过滤器链 |
| 打卡粒度 | 按计划粒度：`check_in_record` 加 `plan_id`，唯一键 `(user_id, plan_id, check_in_date)` 三元组天然防并发重打 |
| 连击归属 | **下沉计划级**（`check_in_plan.current_streak/max_streak`）；`user.current_streak` 语义为"全勤连击"（当天全部进行中计划都完成） |
| 猫的生成与善后 | 创建计划自动生成（勇士/法师/射手随机）；随机名库命名、可改名；删除计划猫跟随逻辑删除 |
| 管理端 | 最小组件：分页查用户 + 封禁/解封（封禁即时生效）；其余数据修正直操作库 |
| 部署 | 单机 Linux + Docker Compose（app + MySQL + Nginx HTTPS）；`local`/`prod` 双 profile，生产密钥走环境变量 |
| 排行榜 | 全勤连击 Top 50，实时查询（`idx_current_streak` 索引即为其而建） |
| 切日口径 | 所有"一天"按北京时间（UTC+8）；补卡月度限额同口径 |
| 展示性字段 | `defense`/`current_hp` 一期无消费场景（无受伤机制），为二期"BOSS 反击/PVP"预留——设计债务而非 bug；`remind_time` 仅存储不推送 |

## 3. 数据模型

### 3.1 原始建表稿（设计者：冉森）

一期启动前已手写 6 张表：`user`（含连击/积分字段）、`check_in_record`、`team`、`group_plan_template`、`check_in_plan`（含任务位图设计：`task_progress` 位图字符串 + `completed_tasks` 冗余）、`cat_spirit`（含 BOSS 战字段）。其中任务位图与小组两张表为后续功能预留。

### 3.2 一期评审修订

| 变更 | 动机 |
|---|---|
| `check_in_record` 加 `plan_id`，唯一键 `(user_id, plan_id, check_in_date)` | 原表无计划维度：每天只能打一次卡，且猫不知道喂的是哪只（多猫方案下必须按计划打卡） |
| `check_in_plan` 加 `current_streak`/`max_streak` | 连击从用户级下沉计划级（多计划各自连击） |
| `cat_spirit` 加 `boss_name` | BOSS 随机名库生成，击杀有叙事感 |
| `check_in_plan` 加 `daily_tasks` JSON | AI 计划草稿的"每日任务"落库位置 |
| `user` 加 `uk_user_account` 唯一键 | 注册防重 |
| **全列名统一 snake_case** | 原稿混用 `isdelete`/`createtime`/`useraccount` 与 `user_id` 两种风格，与 MyBatis Plus 默认驼峰映射冲突（实现期发现的 `Unknown column 'is_delete'` 即此因） |

## 4. 核心玩法数值（集中 `constant/GameConstants`，调平衡改这里）

- **事件池**（每次打卡触发一个）：攻击 BOSS 70% / 属性提升 25%（攻/防/HP 随机 +2~5）/ 暴击 5%（伤害 ×2）
- **伤害** = 攻击力 × (1 + 计划连击 × 2%) × (0.8~1.2 随机)；BOSS 满血 = 100 × 等级^1.3；击败后等级 +1、换新满血 BOSS，血量永不自动回复
- **经验**：打卡 +10、击败 BOSS 额外 +20×BOSS 等级；升级需求 = 50 × 等级^1.5；升级时攻/防/HP 各 +10% 并回满血
- **积分**：打卡 +10、连击每满 7 天 +20、击败 BOSS +10×BOSS 等级；补卡 -50
- **全勤连击**：当天全部进行中计划都有记录时，按昨日是否全勤 +1 或重置为 1（以当前活跃计划数为基准的近似实现，不回溯跨日边界）
- 补卡从最近打卡日回溯重算计划连击（补上缺口可恢复断链）；只能补最近 30 天内、计划创建后的日期

## 5. 认证与权限

注册（账号 4~32 位/密码 8~32 位/两录一致）→ 登录签发 JWT（7 天）→ `JwtInterceptor` 校验。权限注解 `@AuthCheck(mustRole="admin")` 支持方法级与类级。封禁语义见二期缓存文档（现为逐出式准即时）。

## 6. AI 集成（DeepSeek）

- **计划草稿**：`POST /ai/plan/draft`，系统提示词要求严格输出 JSON（名称/类型/描述/目标天数/每日任务）；解析容忍 Markdown 包裹与前后杂文本，字段全部钳制（类型 0~3、天数 1~365、任务 ≤5 条各 ≤30 字），解析失败报"请重试或手动创建"——AI 只做草稿生成器
- **猫口吻鼓励语**：打卡/审核通过响应内携带；虚拟线程限时 4s，超时/异常降级本地语录库随机一条

## 7. 接口清单（一期 14 端点）

用户：`register` / `login` / `me`；计划：创建（自动生成猫）/ 列表 / 详情（含 BOSS 血条）/ 更新 / 删除 / 猫改名；打卡：打卡（返回事件结果）/ 补卡 / 记录日历；AI：计划草稿；排行：`GET /rank/streak`；管理：用户分页 / 封禁解封。

## 8. 实现期工程修复（留档）

1. **MyBatis Plus 与 Spring Boot 4 不兼容**：脚手架的 `mybatis-plus-boot-starter 3.5.2`（Boot 2 时代）在 Mapper 存在时启动报 `Invalid value type for attribute 'factoryBeanObjectType'`。升级 `mybatis-plus-spring-boot4-starter 3.5.17`（自带 mybatis-spring 4.0.0），并补被拆分的 `mybatis-plus-jsqlparser`（分页拦截器所在）。注意 3.5.9+ 的包迁移：`IService/ServiceImpl` 在 `com.baomidou.mybatisplus.spring.service(.impl)`
2. **列名风格冲突**：见 3.2，全表 snake_case 化重建
3. **Windows curl 双坑**：中文件请求体必须 UTF-8 文件 + `--data-binary`；multipart 路径需 `cygpath -w` 转换
4. 冒烟中修复 `createTime` 不回显（insert 后回读填充 DB 默认值）

## 9. 部署

多阶段 `Dockerfile`（TZ=Asia/Shanghai）；`docker-compose.yml`（MySQL 8.4 + app + Nginx + Redis，见二期/缓存文档的增量）；`application-prod.yml` 全环境变量；`deploy/nginx.conf` 预留 HTTPS 证书位。冒烟验收：注册 → 登录 → 建计划（自动领养猫）→ 打卡事件 → 重复打卡拒绝 → 补卡（连击恢复为 2）→ 排行 → 封禁即时生效 → DeepSeek 真实草稿，17 步全通过。
