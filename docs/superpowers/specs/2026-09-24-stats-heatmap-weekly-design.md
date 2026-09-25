# 打卡统计（年度热力图 + 本周周报 + AI 总结）设计文档

- 日期：2026-09-24
- 状态：已实现并冒烟通过

## 一、需求与口径

统计周报采用「数据 + AI 文案」：纯聚合数据打底，AI 生成 2~3 句总结（失败降级模板文案）。热力图为年度 365 格色阶（GitHub 风格）。

## 二、接口（StatsController，/check_in/stats，需登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/heatmap?year=` | 当年每日 `{date, count}`（正常+补卡），不传年份默认当年；固定返回全年 365/366 格 |
| GET | `/weekly` | 本周（周一~今天）周报：perDay 每日计数、perPlan 每个进行中计划本周完成天数、totalCheckins/lastWeekTotal 对比、currentFullStreak（全勤连击）/bestPlanStreak（最强计划连击）、aiSummary |

AI 总结：`AiAssistantService.generateWeeklySummary(统计数据文本)`，复用虚拟线程 + 超时降级模式（8 秒超时，失败返回空串由调用方拼模板文案"本周共打卡 X 次，比上周多/少 Y 次"）。提示词 `WEEKLY_SUMMARY_SYSTEM_PROMPT`（轻松温暖、2~3 句、无格式符号）。

## 三、冒烟结果

- heatmap 365 格、当日计数非零 ✓
- weekly：weekStart=周一、perDay 截至今天、perPlan 含全部进行中计划（含死斗影子计划）本周天数、lastWeek 对比 ✓
- AI 总结实调返回（DeepSeek）："这周你打卡了1次……下周别急，挑一个最想坚持的计划……" ✓

## 四、明确不做

历史活跃计划数回溯（全勤口径按当日近似）、按月/自定义区间周报、AI 总结人工反馈
