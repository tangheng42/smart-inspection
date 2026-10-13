-- 智巡云数据库初始化脚本：用户登录最小表结构
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS `smart_inspection`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

ALTER DATABASE `smart_inspection`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `smart_inspection`;

CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`      VARCHAR(32)  NOT NULL COMMENT '登录账号',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码摘要，禁止保存明文密码',
    `display_name`  VARCHAR(50)  NOT NULL COMMENT '用户展示名称',
    `role_code`     VARCHAR(20)  NOT NULL COMMENT '角色编码：ADMIN/INSPECTOR/MAINTAINER',
    `enabled`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    CONSTRAINT `chk_sys_user_role_code`
        CHECK (`role_code` IN ('ADMIN', 'INSPECTOR', 'MAINTAINER')),
    CONSTRAINT `chk_sys_user_enabled`
        CHECK (`enabled` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '系统用户表';
