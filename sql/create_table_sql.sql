create database if not exists `yu_picture` default character set utf8mb4 collate utf8mb4_unicode_ci;

use `yu_picture`;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;


-- 密码都是 11111111

INSERT INTO user (userAccount, userPassword, userName, userAvatar, userProfile, userRole, editTime, createTime,
                  updateTime, isDelete)
VALUES ('user001', 'f8de235116ca2ec0b8ee885b5c743072', 'Alice', 'https://example.com/avatar1.png', '喜欢编程和音乐',
        'user', NOW(), NOW(), NOW(), 0),
       ('user002', 'f8de235116ca2ec0b8ee885b5c743072', 'Bob', 'https://example.com/avatar2.png', '热爱旅行和美食',
        'user', NOW(), NOW(), NOW(), 0),
       ('admin001', 'f8de235116ca2ec0b8ee885b5c743072', 'Admin', 'https://example.com/avatar3.png', '系统管理员',
        'admin', NOW(), NOW(), NOW(), 0),
       ('user003', 'f8de235116ca2ec0b8ee885b5c743072', 'Charlie', NULL, '喜欢读书和电影', 'user', NOW(), NOW(), NOW(),
        0),
       ('user004', 'f8de235116ca2ec0b8ee885b5c743072', 'Daisy', 'https://example.com/avatar4.png', NULL, 'user', NOW(),
        NOW(), NOW(), 0),
       ('user005', 'f8de235116ca2ec0b8ee885b5c743072', 'Ethan', 'https://example.com/avatar5.png', '运动达人，热爱篮球',
        'user', NOW(), NOW(), NOW(), 0),
       ('user006', 'f8de235116ca2ec0b8ee885b5c743072', NULL, NULL, '神秘用户', 'user', NOW(), NOW(), NOW(), 0),
       ('user007', 'f8de235116ca2ec0b8ee885b5c743072', 'Fiona', 'https://example.com/avatar6.png', '喜欢拍照和画画',
        'user', NOW(), NOW(), NOW(), 0),
       ('user008', 'f8de235116ca2ec0b8ee885b5c743072', 'George', NULL, NULL, 'user', NOW(), NOW(), NOW(), 0),
       ('user009', 'f8de235116ca2ec0b8ee885b5c743072', 'Hannah', 'https://example.com/avatar7.png', '喜欢烹饪和园艺',
        'user', NOW(), NOW(), NOW(), 0);

-- 图片表
create table if not exists picture
(
    id            bigint auto_increment comment 'id' primary key,
    urls          JSON                               NOT NULL COMMENT '用户 URL 信息 (url, thumbUrl, transferUrl)',
    name          varchar(128)                       not null comment '图片名称',
    introduction  varchar(512)                       null comment '简介',
    category      varchar(64)                        null comment '分类',
    tags          varchar(512)                       null comment '标签（JSON 数组）',
    picColor      varchar(16)                        null comment '图片主色调',
    picSize       bigint                             null comment '图片体积',
    picWidth      int                                null comment '图片宽度',
    picHeight     int                                null comment '图片高度',
    picScale      double                             null comment '图片宽高比例',
    picFormat     varchar(32)                        null comment '图片格式',
    spaceId       bigint                             null comment '空间 id（为空表示公共空间）',
    reviewStatus  INT      DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage VARCHAR(512)                       NULL COMMENT '审核信息',
    reviewerId    BIGINT                             NULL COMMENT '审核人 ID',
    reviewTime    DATETIME                           NULL COMMENT '审核时间',
    userId        bigint                             not null comment '创建用户 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId),             -- 提升基于用户 ID 的查询性能
    INDEX idx_reviewStatus (reviewStatus), -- 创建基于 reviewStatus 列的索引
    INDEX idx_spaceId (spaceId)            -- 创建基于 spaceId 列的索引
) comment '图片' collate = utf8mb4_unicode_ci;

# 把之前 url 和 thumbnailUrl 数据写入到 urls
UPDATE picture
SET urls = JSON_OBJECT(
        'url', COALESCE(url, ''),
        'thumbUrl', COALESCE(thumbnailUrl, '')
           )
WHERE picture.url IS NOT NULL
   OR picture.thumbnailUrl IS NOT NULL;

-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;
