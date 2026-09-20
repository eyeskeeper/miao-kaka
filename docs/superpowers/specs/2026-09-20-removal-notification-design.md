# 被移除通知（通用通知基建第一期）设计文档

- 日期：2026-09-20
- 状态：已实现并冒烟通过
- 关联：2026-09-15-admin-leader-economy-design.md（每日即退模型与移除结算）

## 一、需求与口径

组长移除组员后，被移除者收到通知。三项口径（问答定版）：

- **落库持久**（区别于拍一拍的 Redis 当日过期——被移除是重要低频事件，隔几天打开 App 也必须看到）
- **已读/未读 + 角标**：is_read 字段 + 列表/未读数/标记已读三接口，读取不清空
- **通用通知表**：type 字段预留，本期仅 type=1 被移除出死斗；未来弹劾结果/审核结果/解散直接复用

通知内容自动组装：标题「你被移出了死斗」+ 正文（死斗名 + 组长昵称 + 退款说明）；ref_id 关联死斗 id（前端可跳转）。时机：在 removeMember **既有事务内**插入（与移除原子）；仅组长移除触发（主动退出/解散不通知）。

## 二、数据模型

```sql
user_notification
  id, user_id, type tinyint (1:被移除出死斗), title varchar(64), content varchar(255),
  ref_id bigint null, is_read tinyint default 0, create_time
  index idx_user_read (user_id, is_read), index idx_user (user_id)
```

## 三、接口（NotificationController，需登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/notification/list?current=&pageSize=` | 我的通知分页（id 倒序，pageSize≤50），读取不清空 |
| GET | `/notification/unread-count` | 未读数（角标） |
| PUT | `/notification/read/{id}` | 单条已读：归属校验（他人通知 50001「通知不存在」）；已读重复调用幂等成功 |
| PUT | `/notification/read-all` | 全部已读，返回本次标记条数 |

## 四、实现要点

- `NotificationService(Impl)`：notify（单条插入，供各业务事务内调用）/ pageMy / unreadCount / markRead / markAllRead；仅依赖自身 Mapper
- `DuelServiceImpl.removeMember` 注入通知服务：事务内按路径组装退款文案（招募中=「押金 X 喵币已全额退还」；进行中=「已退还剩余 N 天份额 Y 喵币，缺勤份额已入奖池」，金额按 elapsed 实算）后 notify —— 与退款/状态变更为同一事务，移除成功必有通知

## 五、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 招募中移除：type=1、正文含死斗名/组长/「押金 100 喵币已全额退还」、refId 正确、unread=1 | ✓ |
| 2 | 进行中移除（第 5 天，T=7）：正文「已退还剩余 2 天份额 28 喵币，缺勤份额已入奖池」——金额随 elapsed 实算正确（当日 09-16 开赛的局，今天 09-20 移除） | ✓ |
| 3 | 已读链路：read → unread=0 → isRead=true；重复调用幂等 | ✓ |
| 4 | 归属校验：B 标记 A 的通知 → 50001「通知不存在」 | ✓ |
| 5 | read-all 返回 1，unread 归零 | ✓ |
| 6 | 原子性：2 次移除恰好 2 条通知 | ✓ |

## 六、明确不做

推送（一期拉取式）、通知删除/清空接口、其他事件类型接入（type 已预留）、通知内容自定义
