# 好友系统（申请-同意 / 围观 / 点赞小鱼干 / 排行 / 动态 feed）设计文档

- 日期：2026-09-24
- 状态：已实现并冒烟通过
- 关联：打卡点赞与商城「小鱼干兑积分」联动

## 一、口径定版（拷问结论）

- 好友：**申请-同意制**；同意后写入**双向两行**（好友判定免 OR）；申请/通过写通知（type=4 好友申请 / type=5 申请通过）；不能加自己；重复申请/已是好友/存在任向待审或好友关系均拒绝；可删除好友（双向删，删后可重新申请）
- 点赞：仅好友可赞、对象=好友的打卡记录；**同一记录仅一次**（uk_record_liker）、点赞者**每日限 5 次**；被赞者 `dried_fish` +1（原子 SQL）
- 小鱼干：`user.dried_fish` 新列；商城 `POST /mall/exchange-fish` **1:1 兑积分**（原子扣减、余额不足拒绝）
- 围观：仅好友；好友的猫（其全部进行中计划的猫：名/等级/BOSS/击败数）+ 好友计划打卡月历（只读，正常+补卡）
- 排行：好友+我按全勤连击倒序（isMe 标识）；feed：好友近 7 天打卡（正常+补卡，限 50 条倒序），含 likeCount/likedByMe

## 二、数据模型

```sql
user_friend    (user_id, friend_id, status 0待同意/1已同意, agree_time, create_time)
               unique uk_pair(user_id, friend_id), index idx_friend(friend_id, status)
check_in_like  (record_id, liker_id, target_id, like_date, create_time)
               unique uk_record_liker(record_id, liker_id), index idx_target_date(target_id, like_date)
user + dried_fish int not null default 0
```

## 三、接口（FriendController，/friend，需登录）

apply {targetUserId} / applications / agree {id} / reject {id} / list / DELETE /{friendUserId} / search?keyword=（账号精确或 id）/ rank / feed / {friendId}/cats / {friendId}/calendar?planId=&month= / like/{recordId}

## 四、冒烟结果

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 搜索账号定位用户 ✓；申请成功；重复申请拒「已存在好友申请或好友关系」 | ✓ |
| 2 | 申请列表（申请人昵称）→ 同意 → 双向两行 84↔82:1、双方列表互见 | ✓ |
| 3 | 排行：好友+我按全勤连击倒序、isMe 正确 | ✓ |
| 4 | feed：L 的两条打卡（正常+补卡）对 B 可见，点赞数/likedByMe 正确 | ✓ |
| 5 | 点赞：likeCount=1、被赞者 dried_fish+1；重复赞「已经点过赞啦」 | ✓ |
| 6 | 日限：第 5 次成功、第 6 次「今日点赞次数已用完（每日 5 次）」 | ✓ |
| 7 | 围观：好友可见猫列表（10 只）与日历（含补卡状态）；非好友「仅好友可以围观」 | ✓ |
| 8 | 兑换：fish 5→0、points 100→105（1:1 精确）；999 兑「小鱼干不足」 | ✓ |

## 五、明确不做

好友上限与拉黑、非好友围观、点赞通知推送（feed 内可见）、feed 深分页
