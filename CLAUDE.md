# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

miao-kaka（喵卡卡）是一个打卡类应用的 Spring Boot 后端：用户创建打卡计划（每计划自动领养一只"猫精灵"），每日按计划打卡触发随机事件（攻击 BOSS / 属性提升 / 暴击），连续打卡获得连击加成，积分可用于补卡。接入 Spring AI（DeepSeek）实现 AI 生成打卡计划草稿与打卡后的猫口吻鼓励语。一期范围：用户认证（JWT）、个人打卡、猫养成、AI 助手、连击排行榜、最小管理端。

二期新增两种打卡形式（设计文档见 `docs/superpowers/specs/2026-09-11-duel-and-focus-design.md`）：

- **习惯死斗模式**：多人组队押金对抗。虚拟"喵币"账户（注册赠 1000，`/wallet` 可查流水）托管，按出勤比例返还、被没收部分逐笔按其余成员天数占比再分配（`utils/SettlementCalculator` 纯函数 + 单测）；打卡需上传照片凭证（`/duel/{id}/check_in`），记录进入待审核态（status=3），组长（或 AI 视觉模型 GLM-4V 预审，配置开关 `app.ai.vision.*`）审核通过那一刻才触发猫事件结算；成员加入即自动创建"影子计划"（`plan_source=2`，绑定 `duel_id`）复用整个打卡引擎，影子计划被护栏封闭（禁普通打卡/补卡/删除/手动暂停）。结算幂等（`duel.settled` 条件更新抢占 + 定时任务 `config/DuelScheduler` + 详情访问 lazy 兜底）；结束时任然待审核的凭证自动视为通过。合规要点：一期用虚拟押金规避资金二清，押金网关抽象待二期换真实支付。
- **10 分钟习惯养成**：后端不可知，前端本地倒计时 + 现有 `POST /check_in`；后端仅 `check_in_plan.plan_mode` 标记列，无任何逻辑。

## 常用命令

使用 Maven Wrapper（Windows 下用 `mvnw.cmd`；本机需 JAVA_HOME 指向 JDK 21）：

- 启动应用：`./mvnw spring-boot:run`（默认 `local` profile）
- 运行测试：`./mvnw test`
- 打包：`./mvnw package -DskipTests`

应用端口 18089，所有接口前缀为 `/api`（`server.servlet.context-path`）。接口文档（springdoc）：`http://localhost:18089/api/swagger-ui.html`。本地 MySQL：`localhost:3306/my_db`（`application.yml`），DeepSeek key 在 `application-local.yml`（已 gitignore）。

## 架构

分层结构（`com.senze.miaokaka`）：

- `controller/` — REST 入口，统一返回 `BaseResponse<T>`（`ResultUtils.success/error`）；`AiController`（AI 计划草稿）、`PlanController`、`CheckInController`、`UserController`、`RankController`、`AdminController`（`@AuthCheck(mustRole="admin")`）
- `service/` + `service/impl/` — 业务逻辑；`CheckInRecordServiceImpl` 是核心：打卡事件引擎（`TransactionTemplate` 编程式事务，AI 鼓励语在事务提交后生成、超时降级本地语录）、补卡（扣积分、自然月 2 次、连击回溯重算）、死斗审核通过结算入口 `settleApprovedCheckIn`（普通打卡与审核通过共用 `applyGrowthAndPoints`）
- `service/DuelService|DuelBattleService|DuelSettlementService` — 死斗生命周期（创建/加入/退出/影子计划/开赛）、打卡凭证与审核、幂等结算
- `service/WalletService` — 喵币账本：原子条件更新扣押金、每笔流水写 `balance_after` 可重放对账
- `service/CacheService` — Redis 缓存薄封装（`app.cache.enabled` 开关，默认关；关闭时行为与无缓存一致）。硬性原则：钱的数据（喵币/押金/审核）绝不缓存；Cache-Aside + 写时逐出 + TTL 兜底；Redis 异常一律降级直读 DB。三个缓存点：`miaokaka:user:{id}`（登录态，密码脱敏，TTL 5min）、`miaokaka:rank:streak:top50`（TTL 5min）、`miaokaka:duel:agg:{id}`（观看者无关聚合层，60s，个性化字段实时拼装防视角泄漏）。用户行/死斗写路径需记得逐出
- `service/StorageService`（本地磁盘实现）/ `VisionReviewService`（GLM-4V 走 OpenAI 兼容端点，RestClient + 虚拟线程限时）
- `mapper/` — MyBatis Plus `BaseMapper`（注意：3.5.17 中 `IService/ServiceImpl` 在 `com.baomidou.mybatisplus.spring.service(.impl)` 包，分页拦截器在独立构件 `mybatis-plus-jsqlparser`）
- `model/entity|dto|vo` — 实体 / 请求 / 响应对象
- `interceptor/JwtInterceptor` — 登录态 + 角色校验，每请求回库取用户（封禁即时生效），登录用户放 request 属性 `user_login`
- `config/` — CORS、MyBatis Plus（分页插件 + `@MapperScan`）、`JwtProperties`、`WebMvcConfig`（拦截器注册与白名单）
- `constant/` — `GameConstants`（全部游戏数值，调平衡改这里）、`NameLibraryConstant`（猫名/BOSS 名/降级语随机库）、`CheckInConstant`（状态与北京时间 `BIZ_ZONE`）
- `aiAssitant/constant/prompt/SchemeDesignPromptConstant` — AI 提示词（计划草稿严格输出 JSON；解析时容忍 Markdown 包裹并对字段钳制）
- `exception/` — `BusinessException` + `GlobalExceptionHandler`（含参数校验、唯一键冲突兜底）；业务校验用 `ThrowUtils.throwIf`
- `sql/create_table.sql` — 建表脚本（MySQL，snake_case）；`Dockerfile` + `docker-compose.yml` + `deploy/nginx.conf` — 部署（prod 配置全走环境变量，参照 `.env.example`）

## 核心玩法数值（摘要）

打卡每计划每天一次（唯一键 `uk_user_plan_date` 兜底）；事件池：攻击 70% / 属性提升 25% / 暴击 5%（伤害 ×2）；伤害 = 攻击力 × (1 + 计划连击×2%) × 0.8~1.2；BOSS 满血 = 100 × 等级^1.3，血量不回复，击败后等级 +1 换新 BOSS；经验：打卡 +10、击败 +20×BOSS 等级，升级需求 50 × 等级^1.5，升级三维 +10% 并回满血；积分：打卡 +10、连击满 7 天 +20、击败 +10×BOSS 等级；补卡 -50（自然月 2 次，恢复连击，不触发事件）。连击为计划级（`check_in_plan.current_streak`）；`user.current_streak` 是"全勤连击"（当天全部进行中计划都完成）。所有"一天"按北京时间切。

## 注意点

- 技术栈：Java 21、Spring Boot 4.1.0、Spring AI 2.0.0（deepseek starter）、MyBatis Plus 3.5.17（`mybatis-plus-spring-boot4-starter`，专为 Boot 4）、Lombok、Hutool、springdoc。
- 认证：注册/登录签发 JWT（Hutool，HS256，密钥在 `jwt.secret`），密码 BCrypt（仅引 `spring-security-crypto`，未引入 Spring Security 过滤器链）。
- `defense`/`current_hp` 一期为展示属性（无受伤/反击机制），`remind_time` 仅存储不推送——均为二期预留，不是 bug。
