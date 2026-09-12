# 喵卡卡 · Redis 缓存引入 设计与验收文档

日期：2026-09-12
状态：已实现并通过量化验收（提交 `3e45222`）
前置：一期打卡引擎、二期死斗模式（`2026-09-11-duel-and-focus-design.md`）、任务勾选（`2026-09-11-task-toggle-design.md`）

## 1. 背景与目标

以性能为目的引入 Redis 缓存。经 grilling 拷问收敛出的核心共识：本项目尚无真实流量与慢查询数据，因此**验收标准必须量化**——同一接口"首查 vs 缓存命中"的响应耗时与 SQL 次数对比，让"性能变好"成为可断言的事实。

## 2. 决策记录（拷问八问）

| 决策点 | 结论 |
|---|---|
| 验收标准 | 高频读接口（排行榜/死斗详情/登录态）首查 vs 命中的耗时与 SQL 次数对比 |
| 钱的红线 | **喵币余额、押金扣减、奖池结算、审核状态绝不缓存**，永远直读 DB（事务内原子操作 + 缓存 = 读到旧余额的风险） |
| 封禁语义 | 从"每请求回库"改为**逐出式准即时**：管理端封禁逐出登录态缓存，毫秒级生效；直接改库本就不是支持路径 |
| 客户端选型 | `spring-boot-starter-data-redis`（Lettuce）+ `StringRedisTemplate` + JSON；不用 Redisson（无分布式锁需求，结算幂等已由 DB 条件更新实现） |
| 失效策略 | Cache-Aside + 写时逐出 + TTL 兜底 |
| 降级原则 | Redis 任何异常只记 warn 并回源 DB，缓存故障 ≠ 可用性故障 |
| 可开关性 | `app.cache.enabled` 默认关；关闭时所有读写直接跳过，行为与无缓存完全一致 |
| key 规范 | `miaokaka:{module}:{id}`，业务代码只经 `CacheService` 薄封装 |

## 3. 缓存点（仅三处，YAGNI 不扩）

| key | 内容 | TTL | 逐出时机 |
|---|---|---|---|
| `miaokaka:user:{id}` | 拦截器登录态用户对象（**密码摘要置空后入库**） | 5min | 用户行任何更新：打卡、补卡、喵币变动、封禁/解封 |
| `miaokaka:rank:streak:top50` | 全勤连击排行榜（`RankCacheData` 包装，规避 List 泛型擦除） | 5min | 打卡结算（含全勤连击刷新）、封禁/解封 |
| `miaokaka:duel:agg:{id}` | 死斗**观看者无关聚合**（挑战 + 成员 + 确认天数） | 60s | 创建/加入/退出/开赛/解散/审核/结算 |

## 4. 关键设计：视角隔离陷阱

`buildDuelVO(duel, userId)` 原实现是**观看者视角相关**的：VO 内含 `myPlanId`、`myRole`、`myStatus`、`pendingCount`。若直接按 `duel:{id}` 缓存整个 VO，用户 A 加入的死斗会把 A 的影子计划 id 泄漏给 B——数据越权。

解法：缓存拆层。Redis 只存观看者无关的聚合（`DuelAggData`：挑战实体 + 成员行 + 天数），每次请求实时拼装个性化字段（一次成员行查询 + 组长视角一次计数，开销极小）。冒烟实测：同一份缓存聚合下 A 读到 `myPlanId:7`/leader、B 读到 `myPlanId:8`/member，隔离正确。

## 5. 实现要点

- `CacheService`：get/put/evict + 开关判断 + 全量 try/catch 降级；内置 **Jackson 3 `JsonMapper`**（Boot 4 已迁移 Jackson 3，不存在 `com.fasterxml ObjectMapper` Bean；Jackson 3 原生支持 java.time）
- 打卡/补卡/喵币变动等用户行写路径统一逐出 `user:{id}`，保证 `/me`、余额读不到旧值
- 结算幂等仍由 DB 条件更新实现，与缓存无关；结算/审核写后逐出聚合 key
- 计划列表等用户级低频读**不缓存**（命中率不划算，刻意不做）

## 6. 配置与部署

- 本地：`application.yml` `app.cache.enabled: true`（本机 Redis 已启动）；SQL 计数用的 mapper debug 日志放在 gitignored 的 `application-local.yml`
- 生产：`application-prod.yml` 走 `CACHE_ENABLED`/`REDIS_HOST` 环境变量；compose 增加 `redis:7-alpine`（healthcheck），app 依赖其健康检查启动

## 7. 量化验收记录（2026-09-12，全部通过）

| 验收项 | 结果 |
|---|---|
| 排行榜耗时 | 首查 0.0997s → 命中 0.004~0.006s（**约 20 倍**） |
| 排行榜 SQL | 首查 +1 条；连续命中零新增 |
| 登录态 | `/me` 首查 0.019s vs 命中 0.013s |
| 逐出实时性 | 打卡 +10 分后 `/me` 立即读到 20 |
| 封禁即时性 | 建立缓存 → 封禁 → **下一请求立即 40101** → 解封立即恢复 |
| 视角隔离 | 缓存聚合下 A=`myPlanId:7`/leader，B=`myPlanId:8`/member |
| 钱实时 | 钱包/押金/审核全程直读 DB |

## 8. 过程记录（踩坑留档）

1. **Boot 4 = Jackson 3**：`CacheService` 初版注入 `com.fasterxml.jackson.databind.ObjectMapper` 导致启动失败（Bean 不存在）。Spring Boot 4 / Spring Framework 7 已迁移至 `tools.jackson.*`。改用内置 `JsonMapper.builder().build()`，零 Bean 依赖
2. **陈旧实例假象**：两次"灵异现象"（全接口恒定 2.2s、新端点 404/旧 token 失效）根因均为**启动失败的旧实例仍占用 18089 端口**，流量打到的是旧构建。清理手段：`Get-NetTCPConnection -LocalPort 18089` 找 PID 全杀后重启
3. **Windows curl 双坑**（再次验证）：中文件请求体必须由 UTF-8 文件经 `--data-binary @file` 传入；multipart 文件路径必须是 Windows 格式（`cygpath -w` 转换）
