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

