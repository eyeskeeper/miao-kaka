# 数据库初始化
# @author <a href="https://github.com/eyeskeeper">冉森</a>


-- 用户表
create table if not exists user
(
    id             bigint auto_increment comment 'id' primary key,
    user_account    varchar(256)                           not null comment '账号',
    user_password   varchar(512)                           not null comment '密码',
    union_id        varchar(256)                           null comment '微信开放平台id',
    phone          varchar(20)                            default null comment '手机号',
    mp_open_id       varchar(256)                           null comment '公众号openid',
    user_name       varchar(256)                           null comment '用户昵称',
    user_avatar     varchar(1024)                          null comment '用户头像',
    user_profile    varchar(512)                           null comment '用户简介',
    user_role       varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    current_streak int          default 0                 comment '全勤连击天数（当天全部进行中计划都完成才累计）',
    total_points   int          default 0                 comment '当前可用积分余额',
    create_time     datetime     default current_timestamp not null comment '创建时间',
    update_time     datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete       tinyint      default 0                 not null comment '是否删除',
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
    status        tinyint                            not null default 0 comment '打卡状态 (0:正常, 1:补卡, 2:异常)',
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
    create_time    datetime     default current_timestamp not null comment '创建时间',
    update_time    datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete      tinyint               default 0 not null comment '是否删除',
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
    create_time    datetime     default current_timestamp not null comment '创建时间',
    update_time    datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete      tinyint               default 0 not null comment '是否删除',
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
    plan_source     tinyint               default 0 not null comment '计划来源 (0:个人创建, 1:小组模板)',
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
    create_time      datetime     default current_timestamp not null comment '创建时间',
    update_time      datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    is_delete        tinyint               default 0 not null comment '是否删除',
    index idx_user_id (user_id),
    index idx_team_id (team_id),
    index idx_template_id (template_id),
    index idx_plan_source (plan_source),
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
    create_time          datetime     default current_timestamp not null comment '创建时间',
    update_time          datetime     default current_timestamp not null on update current_timestamp comment '更新时间',
    unique key uk_plan_id (plan_id),
    index idx_level (level),
    index idx_boss_level (boss_level)
) comment='猫精灵表' collate = utf8mb4_unicode_ci;
