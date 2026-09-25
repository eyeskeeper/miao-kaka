# 打卡提醒（应用内通知 + App 端本地弹窗）设计文档

- 日期：2026-09-24
- 状态：已实现并冒烟通过
- 关联：2026-09-11-duel-and-focus-design.md（remind_time 一期预留字段）

## 一、背景与形态选择

`check_in_plan.remind_time` 一期只存不推送。项目当前**无任何推送渠道**（manifest 无微信 appid、无个推 SDK），拷问定版：本地 APK 打包 → 采用「后端通知中心（拉模式兜底）+ App 端本地通知（进程存活时系统弹窗）」组合，微信订阅消息与离线推送明确不做（需外部申请）。

## 二、实现

- `NotificationConstant.TYPE_CHECKIN_REMIND = 2`
- 新增 `config/ReminderScheduler`：`@Scheduled(cron = "0 * * * * ?")` 每分钟（北京时间）
  - 查询 `status=ACTIVE AND remind_time = 当前 HH:mm` 的计划
  - 逐计划双重过滤：当日无任意状态打卡记录（已打卡不打扰）；今日尚未发过提醒（查 `user_notification` type=2 + ref_id=plan_id + create_time≥今日零点，天然跨天去重）
  - 命中 → 通知中心「打卡提醒：『计划名』今天的打卡还没完成，喵喵在等你！」（ref_id=planId，前端可跳转）
- 前端 App 端（`pages/index/index.vue`，`#ifdef APP-PLUS` 条件编译）：首页 onShow 拉到计划后，对「已设提醒时间、已过时刻、当日未打卡」的计划调 `uni.createPushMessage` 弹系统通知栏；H5/小程序静默跳过

## 三、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | remind_time 设为下一分钟 → 调度器到点写通知（type=2/ref=计划id），角标 +1 | ✓ |
| 2 | 去重：后续分钟不再重复发送（通知条数恒 1） | ✓ |

## 四、明确不做

微信订阅消息/离线推送（进程被杀场景需个推 SDK）、提醒重试、提醒时间批量修改
