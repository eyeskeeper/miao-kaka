# 数据库初始化
# @author <a href="https://github.com/eyeskeeper">冉森</a>


-- 用户表
create table if not exists user
(
    id             bigint auto_increment comment 'id' primary key,
    user_account   varchar(256)                           not null comment '账号',
    user_password  varchar(512)                           not null comment '密码',
    union_id       varchar(256)                           null comment '微信开放平台id',
    phone          varchar(20)                            default null comment '手机号',
    mp_open_id     varchar(256)                           null comment '公众号openid',
    user_name      varchar(256)                           null comment '用户昵称',
    user_avatar    varchar(1024)                          null comment '用户头像',
    user_profile   varchar(512)                           null comment '用户简介',
    user_role      varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    current_streak int          default 0                 comment '全勤连击天数（当天全部进行中计划都完成才累计）',
    total_points   int          default 0                 comment '当前可用积分余额',
    miao_coins     int          default 1000              comment '喵币余额（押金货币，注册赠送1000）',
    nudge_text     varchar(20)                            default null comment '拍一拍模板文案（空则用系统默认）',
    create_time    datetime     default current_timestamp not null comment '创建时间',
    update_time    datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete      tinyint      default 0                 not null comment '是否删除',
    unique key uk_user_account (user_account),
    index idx_unionid (union_id),
    index idx_phone (phone),
    index idx_current_streak (current_streak)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 2. 创建打卡记录表（按计划粒度打卡，三元组唯一键天然防重）
create table `check_in_record`
(
    record_id     bigint auto_increment comment '打卡记录id' primary key,
    user_id       bigint                             not null comment '关联用户id',
    plan_id       bigint                             not null comment '关联打卡计划id',
    check_in_date date                               not null comment '打卡日期（北京时间）',
    check_in_time datetime                           not null default current_timestamp comment '具体打卡时间点',
    status        tinyint                            not null default 0 comment '打卡状态 (0:正常, 1:补卡, 2:异常, 3:待审核)',
    remark        varchar(255)                       default null comment '打卡备注',
    unique key `uk_user_plan_date` (`user_id`, `plan_id`, `check_in_date`),
    index idx_plan_date (plan_id, check_in_date)
) comment='打卡记录表' collate = utf8mb4_unicode_ci;

-- 3. 小组表（二期）
create table `team`
(
    id            bigint       not null auto_increment comment '小组id' primary key,
    team_name     varchar(128) not null comment '小组名称',
    team_avatar   varchar(1024)         default null comment '小组封面/头像',
    team_type     tinyint               default 0 not null comment '小组类型 (0:学习小组, 1:运动小组, 2:阅读小组, 3:其他)',
    description   varchar(512)          default null comment '小组简介/描述',
    leader_id     bigint       not null comment '组长id',
    location      varchar(255)          default null comment '所属地点',
    status        tinyint               default 0 not null comment '状态 (0:正常, 1:已解散, 2:已封禁)',
    create_time   datetime     default current_timestamp not null comment '创建时间',
    update_time   datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete     tinyint               default 0 not null comment '是否删除',
    index idx_leader_id (leader_id),
    index idx_status (status),
    index idx_team_type (team_type)
) comment='小组表' collate = utf8mb4_unicode_ci;

-- 4. 小组计划模板表（二期）
create table `group_plan_template`
(
    id            bigint       not null auto_increment comment '模板id' primary key,
    team_id       bigint       not null comment '关联小组id',
    template_name varchar(128) not null comment '模板名称',
    template_desc varchar(512)          default null comment '模板描述',
    plan_type     tinyint               default 0 not null comment '计划类型 (0:学习, 1:运动, 2:阅读, 3:其他)',
    target_days   int                   default 0 comment '目标连续打卡天数',
    daily_tasks   json                  default null comment '每日任务配置 (JSON数组)',
    total_tasks   int                   default 1 comment '总任务数',
    is_default    tinyint               default 0 not null comment '是否为小组默认模板 (0:否, 1:是)',
    create_time   datetime     default current_timestamp not null comment '创建时间',
    update_time   datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete     tinyint               default 0 not null comment '是否删除',
    index idx_team_id (team_id),
    index idx_is_default (is_default)
) comment='小组计划模板表' collate = utf8mb4_unicode_ci;

-- 5. 打卡计划表（连击为计划级）
create table `check_in_plan`
(
    id              bigint       not null auto_increment comment '计划id' primary key,
    user_id         bigint       not null comment '用户id',
    team_id         bigint                default null comment '关联小组id（二期）',
    template_id     bigint                default null comment '引用的小组模板id（二期）',
    plan_source     tinyint               default 0 not null comment '计划来源 (0:个人创建, 1:小组模板, 2:死斗挑战)',
    plan_mode       tinyint               default 0 not null comment '计划模式 (0:普通, 1:10分钟习惯，纯标记由前端实现计时)',
    duel_id         bigint                default null comment '关联死斗id（影子计划专属）',
    plan_name       varchar(128) not null comment '计划名称',
    plan_desc       varchar(512)          default null comment '计划描述',
    plan_type       tinyint               default 0 not null comment '计划类型 (0:学习, 1:运动, 2:阅读, 3:其他)',
    target_days     int                   default 0 comment '目标连续打卡天数',
    remind_time     varchar(5)            default null comment '提醒时间 (HH:mm，一期不推送，仅存储)',
    daily_tasks     json                  default null comment '每日任务列表 (JSON数组，AI 草稿/用户自定义)',
    current_streak  int                   default 0 not null comment '当前连续打卡天数（计划级）',
    max_streak      int                   default 0 not null comment '历史最长连续打卡天数（计划级）',
    total_tasks     int                   default 1 comment '总任务数',
    task_progress   varchar(512)          default '' comment '任务完成状态位图，第i位为1表示第i个任务已完成，如"00101"表示第1、3任务完成',
    completed_tasks int                   default 0 comment '已完成任务数（冗余字段，便于查询）',
    completion_time datetime              default null comment '全部完成时间',
    status          tinyint               default 0 not null comment '状态 (0:进行中, 1:已暂停, 2:已结束, 3:已完成)',
    create_time     datetime     default current_timestamp not null comment '创建时间',
    update_time     datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete       tinyint               default 0 not null comment '是否删除',
    index idx_user_id (user_id),
    index idx_team_id (team_id),
    index idx_template_id (template_id),
    index idx_plan_source (plan_source),
    index idx_duel_id (duel_id),
    index idx_status (status)
) comment='打卡计划表' collate = utf8mb4_unicode_ci;

-- 6. 猫精灵表（每计划一只猫，多猫方案）
create table `cat_spirit`
(
    id                  bigint       not null auto_increment comment '猫精灵id' primary key,
    plan_id             bigint       not null comment '关联计划id',
    cat_name            varchar(64)  not null comment '猫精灵名称',
    cat_avatar          varchar(1024)         default null comment '猫精灵头像/形象',
    cat_type            tinyint               default 0 not null comment '猫精灵类型 (0:勇士猫, 1:法师猫, 2:射手猫)',
    level               int                   default 1 not null comment '等级',
    experience          int                   default 0 not null comment '经验值',
    attack              int                   default 10 not null comment '攻击力',
    defense             int                   default 10 not null comment '防御力（一期展示属性，二期战斗消费）',
    max_hp              int                   default 100 not null comment '最大生命值（一期展示属性，二期战斗消费）',
    current_hp          int                   default 100 not null comment '当前生命值（一期展示属性，二期战斗消费）',
    boss_level          int                   default 1 not null comment '当前挑战的BOSS等级',
    boss_name           varchar(64)           default '' not null comment '当前BOSS名称（随机名库生成）',
    boss_hp             int                   default 100 not null comment '当前BOSS剩余血量（不自动回复）',
    boss_max_hp         int                   default 100 not null comment '当前BOSS最大血量',
    total_boss_defeated int                   default 0 not null comment '累计击败BOSS数量',
    create_time         datetime     default current_timestamp not null comment '创建时间',
    update_time         datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    unique key uk_plan_id (plan_id),
    index idx_level (level),
    index idx_boss_level (boss_level)
) comment='猫精灵表' collate = utf8mb4_unicode_ci;

-- 7. 喵币流水表（余额只随流水变动，balance_after 可重放对账）
create table `coin_transaction`
(
    id            bigint auto_increment comment '流水id' primary key,
    user_id       bigint                             not null comment '用户id',
    type          tinyint                            not null comment '类型 (0:注册赠送, 1:押金支出, 2:退还, 3:奖池分得, 4:管理员调整)',
    amount        int                                not null comment '变动数额（正收入/负支出）',
    balance_after int                                not null comment '变动后余额',
    biz_id        bigint                             default null comment '关联业务id（死斗id）',
    remark        varchar(255)                       default null comment '备注',
    create_time   datetime default current_timestamp not null comment '创建时间',
    index idx_user (user_id),
    index idx_biz (biz_id)
) comment='喵币流水表' collate = utf8mb4_unicode_ci;

-- 8. 习惯死斗挑战表
create table `duel`
(
    id                  bigint       not null auto_increment comment '死斗id' primary key,
    mode                tinyint               default 0 not null comment '玩法模式 (0:押金死斗, 1:组队打卡)',
    duel_name           varchar(128) not null comment '死斗名称',
    duel_desc           varchar(512)          default null comment '死斗描述',
    daily_tasks         json                  default null comment '每日任务清单（JSON 数组字符串，AI 草稿/组长自定义；空=无任务清单）',
    leader_id           bigint       not null comment '组长id（创建者）',
    join_mode           tinyint               default 0 not null comment '加入模式 (0:自由加入, 1:审批加入)',
    hidden              tinyint               default 0 not null comment '是否隐藏 (0:公开, 1:隐藏；隐藏局不在招募大厅出现，仅可通过组号/邀请海报发现)',
    deposit_per_member  int          not null comment '每人押金（喵币，押金死斗 100~5000；组队打卡存 0）',
    total_days          int          not null comment '挑战天数（3~365）',
    start_date          date         not null comment '开始日期（北京时间，最早为明天）',
    end_date            date         not null comment '结束日期 = 开始日期 + total_days - 1',
    status              tinyint               default 0 not null comment '状态 (0:招募中, 1:进行中, 2:已结算, 3:已解散)',
    member_count        int                   default 1 not null comment '当前人数（冗余）',
    max_members         int                   default 50 not null comment '人数上限（2~50，创建时组长确定；存量局默认 50）',
    total_pool          int                   default 0 not null comment '当前奖池总额（人数×押金，冗余）',
    removed_pool        int                   default 0 not null comment '被移除成员罚没池（结算时按剩余成员天数占比分配）',
    settled             tinyint               default 0 not null comment '结算是否完成（幂等标记）',
    create_time         datetime     default current_timestamp not null comment '创建时间',
    update_time         datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete           tinyint               default 0 not null comment '是否删除',
    index idx_leader (leader_id),
    index idx_status (status),
    index idx_end_date (end_date)
) comment='习惯死斗挑战表' collate = utf8mb4_unicode_ci;

-- 9. 死斗成员表
create table `duel_member`
(
    id            bigint auto_increment comment '成员id' primary key,
    duel_id       bigint                             not null comment '死斗id',
    user_id       bigint                             not null comment '用户id',
    plan_id       bigint                             not null comment '影子计划id（复用打卡引擎）',
    deposit       int                                not null comment '本人押金（喵币快照）',
    refunded      int                                default 0 not null comment '累计已退金额（每日退还+移除退款）',
    checkin_days  int                                default 0 not null comment '已确认打卡天数（结算时重算）',
    status        tinyint                            default 0 not null comment '状态 (0:已加入, 1:进行中, 2:已结算, 3:已退出, 4:已移除)',
    join_time     datetime default current_timestamp not null comment '加入时间',
    unique key uk_duel_user (duel_id, user_id),
    index idx_user (user_id),
    index idx_plan (plan_id)
) comment='死斗成员表' collate = utf8mb4_unicode_ci;

-- 10. 打卡凭证与审核表（死斗专属）
create table `check_in_evidence`
(
    id             bigint auto_increment comment '凭证id' primary key,
    record_id      bigint                             not null comment '对应打卡记录id（唯一）',
    duel_id        bigint                             not null comment '死斗id',
    user_id        bigint                             not null comment '打卡用户id',
    image_path     varchar(1024)                      not null comment '凭证图片存储路径（相对）',
    image_preview_path varchar(1024)                  default null comment '预览图路径（服务端压缩小图，历史数据为空则回退原图）',
    review_status  tinyint                            default 0 not null comment '审核状态 (0:待审核, 1:通过, 2:驳回)',
    reviewer_id    bigint                             default null comment '审核人id（组长）',
    is_self_review tinyint                            default 0 not null comment '是否组长自审（公示标记）',
    ai_suggestion  tinyint                            default null comment 'AI建议 (空:未启用, 0:建议通过, 1:建议驳回)',
    ai_reason      varchar(512)                       default null comment 'AI 判断理由',
    review_remark  varchar(255)                       default null comment '审核备注（驳回必填）',
    review_time    datetime                           default null comment '审核时间',
    create_time    datetime default current_timestamp not null comment '创建时间',
    unique key uk_record (record_id),
    index idx_duel_status (duel_id, review_status)
) comment='打卡凭证与审核表' collate = utf8mb4_unicode_ci;

-- 11. 死斗加入申请表（审批加入模式）
create table `duel_join_request`
(
    id           bigint auto_increment comment '申请id' primary key,
    duel_id      bigint                             not null comment '死斗id',
    user_id      bigint                             not null comment '申请人id',
    inviter_id   bigint                             default null comment '邀请人id（扫码邀请走申请时留痕）',
    status       tinyint                            default 0 not null comment '状态 (0:待审核, 1:已通过, 2:已拒绝)',
    review_remark varchar(255)                      default null comment '审核备注（拒绝原因/喵币不足自动拒绝）',
    review_time  datetime                           default null comment '审核时间',
    create_time  datetime default current_timestamp not null comment '申请时间',
    index idx_duel_status (duel_id, status),
    index idx_user (user_id)
) comment='死斗加入申请表' collate = utf8mb4_unicode_ci;

-- 死斗弹劾表（组员弹劾组长投票）
create table `duel_impeachment`
(
    id           bigint auto_increment comment '弹劾id' primary key,
    duel_id      bigint                            not null comment '死斗id',
    initiator_id bigint                            not null comment '发起人id（成功后成为新组长）',
    reason       varchar(20)                       not null comment '弹劾原因（20字内）',
    status       tinyint default 0                 not null comment '状态 (0:进行中, 1:成功, 2:失败)',
    fail_reason  varchar(64)                       default null comment '失败原因（维持票过半/投票截止未过半/组长已变更/发起人已离局）',
    impeach_cnt  int     default 0                 not null comment '弹劾票数',
    maintain_cnt int     default 0                 not null comment '维持票数',
    expire_time  datetime                          not null comment '投票截止时间（发起+24小时）',
    finish_time  datetime                          default null comment '结束时间',
    create_time  datetime default current_timestamp not null comment '发起时间',
    index idx_duel_status (duel_id, status)
) comment='死斗弹劾表' collate = utf8mb4_unicode_ci;

-- 死斗弹劾投票表（一人一票，不可改票）
create table `duel_impeachment_vote`
(
    id             bigint auto_increment comment '投票id' primary key,
    impeachment_id bigint                             not null comment '弹劾id',
    duel_id        bigint                             not null comment '死斗id',
    user_id        bigint                             not null comment '投票人id',
    vote           tinyint                            not null comment '投票 (0:维持, 1:弹劾)',
    create_time    datetime default current_timestamp not null comment '投票时间',
    unique key uk_impeach_user (impeachment_id, user_id),
    index idx_duel (duel_id),
    index idx_user (user_id)
) comment='死斗弹劾投票表' collate = utf8mb4_unicode_ci;

-- 死斗邀请码表（每个成员每局一个固定码，海报复用）
create table `duel_invite`
(
    id         bigint auto_increment comment '邀请id' primary key,
    duel_id    bigint                             not null comment '死斗id',
    inviter_id bigint                             not null comment '邀请人id（成员/组长）',
    code       varchar(16)                        not null comment '邀请码（8位大写字母数字，剔除易混字符）',
    poster_url varchar(255)                       default null comment '邀请海报 URL（首次生成后回填复用）',
    create_time datetime default current_timestamp not null comment '创建时间',
    unique key uk_code (code),
    unique key uk_duel_inviter (duel_id, inviter_id),
    index idx_duel (duel_id)
) comment='死斗邀请码表' collate = utf8mb4_unicode_ci;

-- 用户通知表（通用通知，type 区分业务：1被移除出死斗，后续事件类型复用）
create table `user_notification`
(
    id          bigint auto_increment comment '通知id' primary key,
    user_id     bigint                             not null comment '接收人id',
    type        tinyint                            not null comment '通知类型 (1:被移除出死斗)',
    title       varchar(64)                        not null comment '标题',
    content     varchar(255)                       not null comment '正文',
    ref_id      bigint                             default null comment '关联业务id（如死斗id）',
    is_read     tinyint      default 0             not null comment '是否已读 (0:未读, 1:已读)',
    create_time datetime default current_timestamp not null comment '创建时间',
    index idx_user_read (user_id, is_read),
    index idx_user (user_id)
) comment='用户通知表' collate = utf8mb4_unicode_ci;

-- 死斗任务清单（2026-09-20 增补：组长创建时带入，成员影子计划复制）
alter table `duel` add column `daily_tasks` json default null comment '每日任务清单（JSON 数组字符串，AI 草稿/组长自定义；空=无任务清单）' after `duel_desc`;

-- 死斗组限制（2026-09-20 增补：组长规定人数上限 2~50、隐藏局不进招募大厅）
alter table `duel` add column `max_members` int not null default 50 comment '人数上限（2~50，创建时组长确定；存量局默认 50）' after `member_count`;
alter table `duel` add column `hidden` tinyint not null default 0 comment '是否隐藏 (0:公开, 1:隐藏；隐藏局不在招募大厅出现，仅可通过组号/邀请海报发现)' after `join_mode`;

-- 玩法模式（2026-09-20 增补：0 押金死斗 / 1 组队打卡（无押金无奖池，进行中可进出））
alter table `duel` add column `mode` tinyint not null default 0 comment '玩法模式 (0:押金死斗, 1:组队打卡)' after `id`;

-- 成就徽章表（2026-09-24 增补：code 定义于 AchievementConstant，一人一徽唯一）
create table `user_achievement`
(
    id          bigint auto_increment comment '记录id' primary key,
    user_id     bigint                             not null comment '用户id',
    code        varchar(32)                        not null comment '徽章编码（如 FIRST_CHECKIN/STREAK_7）',
    unlocked_at datetime default current_timestamp not null comment '解锁时间',
    unique key uk_user_code (user_id, code),
    index idx_user (user_id)
) comment='用户成就徽章表' collate = utf8mb4_unicode_ci;

-- 积分商城背包表（2026-09-24 增补：item_code 定义于 MallConstant）
create table `user_item`
(
    id          bigint auto_increment comment '记录id' primary key,
    user_id     bigint                             not null comment '用户id',
    item_code   varchar(32)                        not null comment '道具编码（如 MAKEUP_VOUCHER/DOUBLE_EXP）',
    quantity    int      default 0                 not null comment '持有数量',
    update_time datetime default current_timestamp not null on update current_timestamp comment '更新时间',
    unique key uk_user_item (user_id, item_code),
    index idx_user (user_id)
) comment='用户道具背包表' collate = utf8mb4_unicode_ci;

-- 好友关系表（2026-09-24 增补：申请-同意制；同意时写入双向两行）
create table `user_friend`
(
    id          bigint auto_increment comment '记录id' primary key,
    user_id     bigint                             not null comment '发起人id',
    friend_id   bigint                             not null comment '接受人id',
    status      tinyint default 0                  not null comment '状态 (0:待同意, 1:已同意)',
    agree_time  datetime                           default null comment '同意时间',
    create_time datetime default current_timestamp not null comment '申请时间',
    unique key uk_pair (user_id, friend_id),
    index idx_friend (friend_id, status)
) comment='好友关系表' collate = utf8mb4_unicode_ci;

-- 打卡点赞表（好友点赞打卡记录，被赞者得小鱼干）
create table `check_in_like`
(
    id          bigint auto_increment comment '点赞id' primary key,
    record_id   bigint                             not null comment '被赞打卡记录id',
    liker_id    bigint                             not null comment '点赞人id',
    target_id   bigint                             not null comment '记录主人id（冗余）',
    like_date   date                               not null comment '点赞日期（日限统计）',
    create_time datetime default current_timestamp not null comment '点赞时间',
    unique key uk_record_liker (record_id, liker_id),
    index idx_target_date (target_id, like_date)
) comment='打卡点赞表' collate = utf8mb4_unicode_ci;

-- 计划模板表（模板市场：官方/用户公开模板，一键套用建计划）
create table `plan_template`
(
    id            bigint       not null auto_increment comment '模板id' primary key,
    creator_id    bigint       not null comment '创建者id（admin 创建即官方模板）',
    template_name varchar(128) not null comment '模板名称',
    template_desc varchar(512)          default null comment '模板描述',
    plan_type     tinyint               default 0 not null comment '计划类型 (0:学习, 1:运动, 2:阅读, 3:其他)',
    target_days   int                   default 21 not null comment '目标连续打卡天数',
    daily_tasks   json                  default null comment '每日任务配置 (JSON数组)',
    total_tasks   int                   default 1 not null comment '总任务数',
    is_official   tinyint               default 0 not null comment '是否官方模板 (0:否, 1:是)',
    use_count     int                   default 0 not null comment '被套用次数',
    is_delete     tinyint               default 0 not null comment '是否删除',
    create_time   datetime     default current_timestamp not null comment '创建时间',
    update_time   datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    index idx_official (is_official),
    index idx_creator (creator_id)
) comment='计划模板表' collate = utf8mb4_unicode_ci;

-- 小鱼干（好友点赞获得，商城 1:1 兑积分）
alter table `user` add column `dried_fish` int not null default 0 comment '小鱼干（好友点赞获得，1:1 兑积分）' after `total_points`;

-- 系统公告表（2026-09-24 增补：admin 发布后广播复制到 user_notification）
create table `announcement`
(
    id         bigint auto_increment comment '公告id' primary key,
    title      varchar(64)                        not null comment '标题',
    content    varchar(512)                       not null comment '正文',
    creator_id bigint                             not null comment '发布人id（admin）',
    create_time datetime default current_timestamp not null comment '发布时间'
) comment='系统公告表' collate = utf8mb4_unicode_ci;
