# 死斗邀请海报（二维码邀请加入）设计文档

- 日期：2026-09-16
- 状态：已实现并冒烟通过
- 关联：2026-09-11-duel-and-focus-design.md（死斗模式）、2026-09-15-admin-leader-economy-design.md（加入审批模式）

## 一、需求与口径定版

用户可以邀请其他人：生成带邀请二维码的海报，别人扫码后即可申请加入小组；**如果是组长邀请则直接加入**。

四项口径（问答定版）：

| 决策点 | 结论 |
| --- | --- |
| 海报生成方 | 后端整图生成（Java2D 750×1000 PNG），走存储服务返回 URL；每（成员，死斗）只渲染一次复用 |
| 二维码内容 | 免登录落地 URL `{app.invite.base-url}/api/duel/invite/info/{码}`（今天就是可用端点，未来接 H5 无需换码）；App 内同时支持手动输码 |
| 生效边界 | 仅招募中（RECRUITING）可生成/使用，开赛后失效；**组长码在自由制与审批制下都直接入组（审批制免审）**；成员码：自由制直接加入、审批制走现有申请流程 |
| 邀请码生命周期 | 8 位大写字母数字（剔除 0/O/1/I），每个（成员，死斗）固定一个长期码，退出重进沿用 |

补充细则：

- 申请路径留痕：成员邀请走审批制时 `duel_join_request.inviter_id` 记录邀请人
- 生成资格：招募中死斗的正式成员（组长与组员都可以）；使用资格：非在册成员（已退出者可重进，与 join 口径一致）
- 满员拦截沿用 join/apply 既有护栏（MEMBER_MAX=50）
- 邀请全程零资金变动（入组押金由既有 join/审批流程收取）

## 二、数据模型

```sql
duel_invite
  id, duel_id, inviter_id, code varchar(16), poster_url varchar(255) null, create_time
  unique uk_code (code), unique uk_duel_inviter (duel_id, inviter_id), index idx_duel (duel_id)

duel_join_request  -- 加列
  inviter_id bigint null  -- 扫码邀请走申请时留痕
```

撞码处理：`uk_code` 冲突时换码重试（≤5 次）；`uk_duel_inviter` 并发双建时按已有行返回。

## 三、接口

| 方法 | 路径 | 登录 | 说明 |
| --- | --- | --- | --- |
| POST | `/duel/{duelId}/invite` | 是 | 生成/获取我的邀请：`{code, posterUrl, duelName, inviterName, qrContent}` |
| GET | `/duel/invite/info/{code}` | **否** | 扫码落地摘要：死斗名/组长/模式/押金/天数/人数/状态/邀请人/`leaderInvite`/`joinAction`（direct/apply）；错码 40400 |
| POST | `/duel/invite/use` | 是 | body `{code}`；返回 `{action: joined/applied, duel: DuelVO}` |

路径安全：`/duel/invite/info/{code}` 比 `GET /duel/{duelId}` 多一段不冲突；literal 优先于变量匹配，`POST /duel/invite/use` 不会被 `/duel/{duelId}/invite` 误吃且**不在白名单**，JWT 照常生效。白名单仅放行 `"/duel/invite/info/*"`（`config/WebMvcConfig`）。

## 四、关键实现

- **海报渲染** `utils/PosterRenderer`：hutool `QrCodeUtil`（底层新增 `com.google.zxing:core` 3.5.3）出二维码 → Graphics2D 合成暖色模板（饰条/标题/死斗名/组长/押金/天数/人数/邀请语/白底二维码/提示语），超宽文本自动省略号截断；中文依赖系统字体，Linux 容器部署需装中文字体
- **存储扩展**：`StorageService.storeImage(byte[], ext)` 新重载（服务端生成图无预览图概念），本地/OSS 双实现
- **免审入组** `DuelService.joinDirect`：跳过审批模式校验（组长码专用），其余护栏（招募中/成员查重/扣押金/影子计划）与 `join` 完全一致
- **useInvite 路由**：组长码 → `joinDirect`；成员码 → 自由制 `join`、审批制 `apply(userId, duelId, inviterId)`（新重载，申请落库写 inviter_id）
- **配置**：`app.invite.base-url`（本地 `http://localhost:18089`，生产 `INVITE_BASE_URL` 环境变量）

## 五、冒烟中发现并修复的两个既有 bug

1. **`/uploads/**` 静态资源 404**（预先存在，凭证预览从未被 HTTP 直连暴露过）：`app.upload-dir` 相对路径 `./data/uploads` 生成的资源 URI 带 `/./` 段，Spring 解析不到 → `WebMvcConfig` 中 `toAbsolutePath().normalize()` 修复（顺手补尾斜杠）
2. **退出后重进必撞唯一键**（预先存在，此前只测过退出未测重进）：`duel_member.uk_duel_user` 下 QUIT 行仍占位，重进走无条件 INSERT 报 duplicate key（被全局异常处理器包装成"重复操作"）→ `createMembershipTx` 改为存在 QUIT 行时**复活原行**（重置押金快照/refunded/checkinDays、换新影子计划、status 回已加入），member_count/total_pool 的减加与退出互为逆操作

另修正一个功能内缺陷：免审路径最初调用 `join()` 会被其审批模式校验拦截 → 拆出 `joinDirect`。

## 六、冒烟结果（F 自由制 / P 审批制 / G 失效测试局）

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 组长生成海报：返回码+URL，PNG 落盘 750×1000（IHDR 0x2EE×0x3E8）、56.9KB、HTTP 可访问 | ✓ |
| 2 | 重复调用：同码同 URL，uploads 目录文件数不变（不重画） | ✓ |
| 3 | 同局成员生成：码/海报均不同，info 显示对应邀请人 | ✓ |
| 4 | 免登录 info：无 token 200；错码 40400 | ✓ |
| 5 | 组长码免审：审批制死斗外人 use → 直接入组（member_count+1、扣押金、无申请行） | ✓ |
| 6 | 成员码（审批制）：applied + `inviter_id=85` 留痕 → 组长批准 → 扣押金入组 | ✓ |
| 7 | 成员码（自由制）：joined 直接入组 | ✓ |
| 8 | 重复使用：在册成员 use → 拒「你已在该死斗中」 | ✓ |
| 9 | 退出者用自己码重进：joined（复活原行，押金重扣，退款互逆） | ✓（修复后） |
| 10 | 开赛后：use → 拒「邀请已失效」；生成 → 拒「仅招募中」 | ✓ |
| 11 | 资金对账：4 账号余额 600/800/700/800（sum 2900），新增 10 笔流水全部为押金/退款，邀请路径零新增资金类型 | ✓ |

## 七、明确不做

微信/小程序深链跳转、H5 落地页美化（当前只出 JSON 摘要）、邀请奖励与传播统计报表、多套海报模板、一次性码
