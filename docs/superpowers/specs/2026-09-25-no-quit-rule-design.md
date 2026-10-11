# 死斗规则改版：一经加入无法退出 设计文档

- 日期：2026-09-25
- 状态：已实现
- 关联：2026-09-11-duel-and-focus-design.md、2026-09-24-team-mode-design.md（组队打卡进行中可退的旧规则被本规则覆盖）

## 一、规则定版

死斗一经加入（两种玩法、任何阶段）**无法退出**：

- 移除 `POST /duel/{id}/quit` 端点与服务方法（前后端全链路）
- 成员离开仅剩两条路径：**组长移除**（removeMember，押金局按规则退款/组队局直接移除）与**人数不足自动解散**（startIfDue，全额退款）
- 组队打卡旧规则「进行中可自由退出」被本规则覆盖
- 好友拉黑不受影响（拉黑只解除好友关系，不解除死斗成员身份）

## 二、实现

- 后端：DuelService/DuelServiceImpl 删除 quit 方法；DuelController 删除 `/quit` 端点
- 前端：api/duel.ts 删 quitDuel；死斗详情页删退出按钮与 doQuit/quitting
- 附带修复：此前一次脚本误删把拍一拍端点的 `@PathVariable targetUserId` 参数一并削掉，已恢复

## 三、明确不做

退出通道翻案、请假/暂停机制、转让后退出（转让是组长权限转移，不是退出）
