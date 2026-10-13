-- 智巡云完整数据库表结构（共 21 张表）
-- 适用版本：MySQL 8.0+
-- 使用说明：本文件可以独立执行，不依赖 001_create_sys_user.sql。
-- 设计原则：只新增缺失表，不删除或覆盖已有数据；脚本可重复执行。

CREATE DATABASE IF NOT EXISTS `smart_inspection`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

ALTER DATABASE `smart_inspection`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `smart_inspection`;

-- 1. 系统用户
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

-- 2. 区域管理
CREATE TABLE IF NOT EXISTS `area`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '区域ID',
    `code`        VARCHAR(30)  NOT NULL COMMENT '区域编码',
    `name`        VARCHAR(50)  NOT NULL COMMENT '区域名称',
    `description` VARCHAR(255) NULL COMMENT '区域说明',
    `enabled`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_area_code` (`code`),
    KEY `idx_area_enabled_name` (`enabled`, `name`),
    CONSTRAINT `chk_area_enabled` CHECK (`enabled` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '设备所属区域';

-- 3. 设备类型
CREATE TABLE IF NOT EXISTS `equipment_type`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备类型ID',
    `code`        VARCHAR(30)  NOT NULL COMMENT '设备类型编码',
    `name`        VARCHAR(50)  NOT NULL COMMENT '设备类型名称',
    `description` VARCHAR(255) NULL COMMENT '设备类型说明',
    `enabled`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_equipment_type_code` (`code`),
    KEY `idx_equipment_type_enabled_name` (`enabled`, `name`),
    CONSTRAINT `chk_equipment_type_enabled` CHECK (`enabled` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '设备类型';

-- 4. 设备台账
CREATE TABLE IF NOT EXISTS `equipment`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `code`               VARCHAR(40)  NOT NULL COMMENT '设备编码',
    `name`               VARCHAR(80)  NOT NULL COMMENT '设备名称',
    `equipment_type_id`  BIGINT       NOT NULL COMMENT '设备类型ID',
    `area_id`            BIGINT       NOT NULL COMMENT '所属区域ID',
    `model`              VARCHAR(80)  NULL COMMENT '设备型号',
    `manufacturer`       VARCHAR(80)  NULL COMMENT '生产厂商',
    `serial_number`      VARCHAR(80)  NULL COMMENT '设备序列号',
    `install_date`       DATE         NULL COMMENT '安装日期',
    `status`             VARCHAR(20)  NOT NULL DEFAULT 'NORMAL'
        COMMENT '状态：NORMAL/FAULT/IN_REPAIR/DISABLED',
    `last_inspection_at` DATETIME     NULL COMMENT '最近一次巡检完成时间',
    `remark`             VARCHAR(500) NULL COMMENT '备注',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_equipment_code` (`code`),
    UNIQUE KEY `uk_equipment_serial_number` (`serial_number`),
    KEY `idx_equipment_type_status` (`equipment_type_id`, `status`),
    KEY `idx_equipment_area_status` (`area_id`, `status`),
    KEY `idx_equipment_last_inspection` (`last_inspection_at`),
    CONSTRAINT `fk_equipment_type` FOREIGN KEY (`equipment_type_id`)
        REFERENCES `equipment_type` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_equipment_area` FOREIGN KEY (`area_id`)
        REFERENCES `area` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_equipment_status`
        CHECK (`status` IN ('NORMAL', 'FAULT', 'IN_REPAIR', 'DISABLED'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '设备台账';

-- 5. 巡检项目模板
CREATE TABLE IF NOT EXISTS `inspection_item`
(
    `id`                   BIGINT         NOT NULL AUTO_INCREMENT COMMENT '巡检项目ID',
    `code`                 VARCHAR(40)    NOT NULL COMMENT '巡检项目编码',
    `name`                 VARCHAR(80)    NOT NULL COMMENT '巡检项目名称',
    `equipment_type_id`    BIGINT         NOT NULL COMMENT '适用设备类型ID',
    `check_method`         VARCHAR(255)   NULL COMMENT '检查方法',
    `standard_description` VARCHAR(500)   NOT NULL COMMENT '合格标准说明',
    `result_type`          VARCHAR(20)    NOT NULL COMMENT '结果类型：BOOLEAN/NUMBER/TEXT',
    `unit`                 VARCHAR(20)    NULL COMMENT '数值单位',
    `min_value`            DECIMAL(12, 4) NULL COMMENT '正常最小值',
    `max_value`            DECIMAL(12, 4) NULL COMMENT '正常最大值',
    `is_required`          TINYINT        NOT NULL DEFAULT 1 COMMENT '是否必填：0否，1是',
    `enabled`              TINYINT        NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    `created_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inspection_item_code` (`code`),
    KEY `idx_inspection_item_type_enabled` (`equipment_type_id`, `enabled`),
    CONSTRAINT `fk_inspection_item_equipment_type` FOREIGN KEY (`equipment_type_id`)
        REFERENCES `equipment_type` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_inspection_item_result_type`
        CHECK (`result_type` IN ('BOOLEAN', 'NUMBER', 'TEXT')),
    CONSTRAINT `chk_inspection_item_required` CHECK (`is_required` IN (0, 1)),
    CONSTRAINT `chk_inspection_item_enabled` CHECK (`enabled` IN (0, 1)),
    CONSTRAINT `chk_inspection_item_range`
        CHECK (`min_value` IS NULL OR `max_value` IS NULL OR `min_value` <= `max_value`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检项目模板';

-- 6. 巡检计划
CREATE TABLE IF NOT EXISTS `inspection_plan`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '巡检计划ID',
    `name`             VARCHAR(100) NOT NULL COMMENT '计划名称',
    `inspector_id`     BIGINT       NOT NULL COMMENT '负责巡检员ID',
    `frequency_type`   VARCHAR(20)  NOT NULL COMMENT '周期：DAILY/WEEKLY/MONTHLY',
    `execution_time`   TIME         NOT NULL COMMENT '计划执行时间',
    `start_date`       DATE         NOT NULL COMMENT '生效日期',
    `end_date`         DATE         NULL COMMENT '失效日期',
    `deadline_hours`   SMALLINT     NOT NULL DEFAULT 24 COMMENT '任务截止小时数',
    `enabled`          TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    `next_generate_at` DATETIME     NULL COMMENT '下次应生成任务的时间',
    `remark`           VARCHAR(500) NULL COMMENT '备注',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inspection_plan_name` (`name`),
    KEY `idx_inspection_plan_generate` (`enabled`, `next_generate_at`),
    KEY `idx_inspection_plan_inspector` (`inspector_id`, `enabled`),
    CONSTRAINT `fk_inspection_plan_inspector` FOREIGN KEY (`inspector_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_inspection_plan_frequency`
        CHECK (`frequency_type` IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT `chk_inspection_plan_deadline`
        CHECK (`deadline_hours` BETWEEN 1 AND 168),
    CONSTRAINT `chk_inspection_plan_enabled` CHECK (`enabled` IN (0, 1)),
    CONSTRAINT `chk_inspection_plan_date_range`
        CHECK (`end_date` IS NULL OR `end_date` >= `start_date`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检计划';

-- WEEKLY 保存1至7，MONTHLY 保存1至28；DAILY 不需要明细行。
CREATE TABLE IF NOT EXISTS `inspection_plan_schedule`
(
    `plan_id`    BIGINT   NOT NULL COMMENT '巡检计划ID',
    `day_value`  TINYINT  NOT NULL COMMENT '周几或每月第几天',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`plan_id`, `day_value`),
    CONSTRAINT `fk_plan_schedule_plan` FOREIGN KEY (`plan_id`)
        REFERENCES `inspection_plan` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `chk_plan_schedule_day` CHECK (`day_value` BETWEEN 1 AND 28)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检计划周期明细';

CREATE TABLE IF NOT EXISTS `inspection_plan_equipment`
(
    `plan_id`      BIGINT   NOT NULL COMMENT '巡检计划ID',
    `equipment_id` BIGINT   NOT NULL COMMENT '设备ID',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`plan_id`, `equipment_id`),
    KEY `idx_plan_equipment_equipment` (`equipment_id`),
    CONSTRAINT `fk_plan_equipment_plan` FOREIGN KEY (`plan_id`)
        REFERENCES `inspection_plan` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_plan_equipment_equipment` FOREIGN KEY (`equipment_id`)
        REFERENCES `equipment` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检计划设备关系';

CREATE TABLE IF NOT EXISTS `inspection_plan_item`
(
    `plan_id`           BIGINT   NOT NULL COMMENT '巡检计划ID',
    `inspection_item_id` BIGINT  NOT NULL COMMENT '巡检项目ID',
    `sort_no`           INT      NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`plan_id`, `inspection_item_id`),
    KEY `idx_plan_item_item` (`inspection_item_id`),
    KEY `idx_plan_item_sort` (`plan_id`, `sort_no`),
    CONSTRAINT `fk_plan_item_plan` FOREIGN KEY (`plan_id`)
        REFERENCES `inspection_plan` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_plan_item_item` FOREIGN KEY (`inspection_item_id`)
        REFERENCES `inspection_item` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检计划项目关系';

-- 7. 定时生成的巡检任务；plan_id + task_date 唯一保证同一计划每天只生成一次。
CREATE TABLE IF NOT EXISTS `inspection_task`
(
    `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '巡检任务ID',
    `task_no`                   VARCHAR(32)  NOT NULL COMMENT '任务编号',
    `plan_id`                   BIGINT       NOT NULL COMMENT '来源巡检计划ID',
    `plan_name_snapshot`        VARCHAR(100) NOT NULL COMMENT '计划名称快照',
    `inspector_id`              BIGINT       NOT NULL COMMENT '巡检员ID',
    `inspector_name_snapshot`   VARCHAR(50)  NOT NULL COMMENT '巡检员名称快照',
    `task_date`                 DATE         NOT NULL COMMENT '任务日期',
    `scheduled_at`              DATETIME     NOT NULL COMMENT '计划执行时间',
    `deadline_at`               DATETIME     NOT NULL COMMENT '截止时间',
    `status`                    VARCHAR(24)  NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/IN_PROGRESS/COMPLETED/OVERDUE/CANCELLED',
    `equipment_count`           INT          NOT NULL DEFAULT 0 COMMENT '设备总数',
    `completed_equipment_count` INT          NOT NULL DEFAULT 0 COMMENT '已完成设备数',
    `started_at`                DATETIME     NULL COMMENT '开始时间',
    `completed_at`              DATETIME     NULL COMMENT '完成时间',
    `cancel_reason`             VARCHAR(500) NULL COMMENT '取消原因',
    `version`                   INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `updated_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inspection_task_no` (`task_no`),
    UNIQUE KEY `uk_inspection_task_plan_date` (`plan_id`, `task_date`),
    KEY `idx_inspection_task_user_status_date` (`inspector_id`, `status`, `task_date`),
    KEY `idx_inspection_task_status_deadline` (`status`, `deadline_at`),
    CONSTRAINT `fk_inspection_task_plan` FOREIGN KEY (`plan_id`)
        REFERENCES `inspection_plan` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_inspection_task_inspector` FOREIGN KEY (`inspector_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_inspection_task_status`
        CHECK (`status` IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED')),
    CONSTRAINT `chk_inspection_task_counts`
        CHECK (`equipment_count` >= 0
            AND `completed_equipment_count` >= 0
            AND `completed_equipment_count` <= `equipment_count`),
    CONSTRAINT `chk_inspection_task_deadline`
        CHECK (`deadline_at` >= `scheduled_at`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检任务';

-- 任务生成时保存设备快照，避免设备改名后历史任务内容发生变化。
CREATE TABLE IF NOT EXISTS `inspection_task_equipment`
(
    `id`                         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '任务设备ID',
    `task_id`                    BIGINT      NOT NULL COMMENT '巡检任务ID',
    `equipment_id`               BIGINT      NOT NULL COMMENT '设备ID',
    `equipment_code_snapshot`    VARCHAR(40) NOT NULL COMMENT '设备编码快照',
    `equipment_name_snapshot`    VARCHAR(80) NOT NULL COMMENT '设备名称快照',
    `equipment_type_id`          BIGINT      NOT NULL COMMENT '设备类型ID',
    `area_id`                    BIGINT      NOT NULL COMMENT '区域ID',
    `area_name_snapshot`         VARCHAR(50) NOT NULL COMMENT '区域名称快照',
    `equipment_status_snapshot`  VARCHAR(20) NOT NULL COMMENT '任务生成时设备状态',
    `completion_status`          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT '设备巡检状态：PENDING/COMPLETED',
    `sort_no`                    INT         NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `completed_at`               DATETIME    NULL COMMENT '该设备巡检完成时间',
    `created_at`                 DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_equipment` (`task_id`, `equipment_id`),
    KEY `idx_task_equipment_equipment` (`equipment_id`),
    CONSTRAINT `fk_task_equipment_task` FOREIGN KEY (`task_id`)
        REFERENCES `inspection_task` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_task_equipment_equipment` FOREIGN KEY (`equipment_id`)
        REFERENCES `equipment` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_task_equipment_type` FOREIGN KEY (`equipment_type_id`)
        REFERENCES `equipment_type` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_task_equipment_area` FOREIGN KEY (`area_id`)
        REFERENCES `area` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_task_equipment_status`
        CHECK (`equipment_status_snapshot` IN ('NORMAL', 'FAULT', 'IN_REPAIR', 'DISABLED')),
    CONSTRAINT `chk_task_equipment_completion`
        CHECK (`completion_status` IN ('PENDING', 'COMPLETED'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检任务设备快照';

-- 同一计划目前只允许选择同一设备类型，因此任务项目可按任务统一保存快照。
CREATE TABLE IF NOT EXISTS `inspection_task_item`
(
    `id`                            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '任务巡检项目ID',
    `task_id`                       BIGINT         NOT NULL COMMENT '巡检任务ID',
    `inspection_item_id`            BIGINT         NOT NULL COMMENT '巡检项目ID',
    `item_code_snapshot`            VARCHAR(40)    NOT NULL COMMENT '项目编码快照',
    `item_name_snapshot`            VARCHAR(80)    NOT NULL COMMENT '项目名称快照',
    `check_method_snapshot`         VARCHAR(255)   NULL COMMENT '检查方法快照',
    `standard_description_snapshot` VARCHAR(500)   NOT NULL COMMENT '标准说明快照',
    `result_type_snapshot`          VARCHAR(20)    NOT NULL COMMENT '结果类型快照',
    `unit_snapshot`                 VARCHAR(20)    NULL COMMENT '单位快照',
    `min_value_snapshot`            DECIMAL(12, 4) NULL COMMENT '最小值快照',
    `max_value_snapshot`            DECIMAL(12, 4) NULL COMMENT '最大值快照',
    `is_required_snapshot`          TINYINT        NOT NULL COMMENT '是否必填快照',
    `sort_no`                       INT            NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `created_at`                    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_item` (`task_id`, `inspection_item_id`),
    KEY `idx_task_item_sort` (`task_id`, `sort_no`),
    CONSTRAINT `fk_task_item_task` FOREIGN KEY (`task_id`)
        REFERENCES `inspection_task` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_task_item_item` FOREIGN KEY (`inspection_item_id`)
        REFERENCES `inspection_item` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_task_item_result_type`
        CHECK (`result_type_snapshot` IN ('BOOLEAN', 'NUMBER', 'TEXT')),
    CONSTRAINT `chk_task_item_required` CHECK (`is_required_snapshot` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检任务项目快照';

-- 8. 每个任务设备产生一条巡检记录。
CREATE TABLE IF NOT EXISTS `inspection_record`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '巡检记录ID',
    `task_id`           BIGINT       NOT NULL COMMENT '巡检任务ID',
    `task_equipment_id` BIGINT       NOT NULL COMMENT '任务设备ID',
    `equipment_id`      BIGINT       NOT NULL COMMENT '设备ID',
    `inspector_id`      BIGINT       NOT NULL COMMENT '实际巡检员ID',
    `normal`            TINYINT      NOT NULL COMMENT '整体是否正常：0否，1是',
    `remark`            VARCHAR(500) NULL COMMENT '巡检备注',
    `completed_at`      DATETIME     NOT NULL COMMENT '巡检完成时间',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inspection_record_task_equipment` (`task_id`, `equipment_id`),
    UNIQUE KEY `uk_inspection_record_task_equipment_id` (`task_equipment_id`),
    KEY `idx_inspection_record_equipment_time` (`equipment_id`, `completed_at`),
    KEY `idx_inspection_record_inspector_time` (`inspector_id`, `completed_at`),
    KEY `idx_inspection_record_normal_time` (`normal`, `completed_at`),
    CONSTRAINT `fk_inspection_record_task` FOREIGN KEY (`task_id`)
        REFERENCES `inspection_task` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_inspection_record_task_equipment` FOREIGN KEY (`task_equipment_id`)
        REFERENCES `inspection_task_equipment` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_inspection_record_equipment` FOREIGN KEY (`equipment_id`)
        REFERENCES `equipment` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_inspection_record_inspector` FOREIGN KEY (`inspector_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_inspection_record_normal` CHECK (`normal` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '设备巡检记录';

CREATE TABLE IF NOT EXISTS `inspection_record_item`
(
    `id`                            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '巡检项目结果ID',
    `inspection_record_id`          BIGINT       NOT NULL COMMENT '巡检记录ID',
    `inspection_item_id`            BIGINT       NOT NULL COMMENT '巡检项目ID',
    `item_code_snapshot`            VARCHAR(40)  NOT NULL COMMENT '项目编码快照',
    `item_name_snapshot`            VARCHAR(80)  NOT NULL COMMENT '项目名称快照',
    `standard_description_snapshot` VARCHAR(500) NOT NULL COMMENT '标准说明快照',
    `result_type_snapshot`          VARCHAR(20)  NOT NULL COMMENT '结果类型快照',
    `result_value`                  VARCHAR(500) NOT NULL COMMENT '实际巡检结果',
    `unit_snapshot`                 VARCHAR(20)  NULL COMMENT '单位快照',
    `normal`                        TINYINT      NOT NULL COMMENT '该项目是否正常：0否，1是',
    `remark`                        VARCHAR(500) NULL COMMENT '项目备注',
    `created_at`                    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_item` (`inspection_record_id`, `inspection_item_id`),
    KEY `idx_record_item_abnormal` (`normal`, `inspection_item_id`),
    CONSTRAINT `fk_record_item_record` FOREIGN KEY (`inspection_record_id`)
        REFERENCES `inspection_record` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_record_item_item` FOREIGN KEY (`inspection_item_id`)
        REFERENCES `inspection_item` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_record_item_result_type`
        CHECK (`result_type_snapshot` IN ('BOOLEAN', 'NUMBER', 'TEXT')),
    CONSTRAINT `chk_record_item_normal` CHECK (`normal` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '巡检项目结果';

-- 9. 异常记录；当前提交模型中每条设备巡检记录最多产生一条异常。
CREATE TABLE IF NOT EXISTS `fault_record`
(
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '异常记录ID',
    `fault_no`             VARCHAR(32)   NOT NULL COMMENT '异常编号',
    `inspection_task_id`   BIGINT        NOT NULL COMMENT '关联巡检任务ID',
    `inspection_record_id` BIGINT        NOT NULL COMMENT '关联巡检记录ID',
    `equipment_id`         BIGINT        NOT NULL COMMENT '异常设备ID',
    `reporter_id`          BIGINT        NOT NULL COMMENT '上报人ID',
    `fault_type`           VARCHAR(50)   NOT NULL COMMENT '异常类型',
    `fault_level`          VARCHAR(20)   NOT NULL COMMENT 'LOW/MEDIUM/HIGH/URGENT',
    `description`          VARCHAR(1000) NOT NULL COMMENT '异常描述',
    `status`               VARCHAR(24)   NOT NULL DEFAULT 'REPORTED'
        COMMENT 'REPORTED/ORDER_CREATED/PROCESSING/RESOLVED/CLOSED',
    `reported_at`          DATETIME      NOT NULL COMMENT '上报时间',
    `resolved_at`          DATETIME      NULL COMMENT '解决时间',
    `closed_at`            DATETIME      NULL COMMENT '关闭时间',
    `version`              INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fault_record_no` (`fault_no`),
    UNIQUE KEY `uk_fault_record_inspection_record` (`inspection_record_id`),
    KEY `idx_fault_record_equipment_status` (`equipment_id`, `status`),
    KEY `idx_fault_record_level_status_time` (`fault_level`, `status`, `reported_at`),
    KEY `idx_fault_record_reporter_time` (`reporter_id`, `reported_at`),
    CONSTRAINT `fk_fault_record_task` FOREIGN KEY (`inspection_task_id`)
        REFERENCES `inspection_task` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_fault_record_inspection_record` FOREIGN KEY (`inspection_record_id`)
        REFERENCES `inspection_record` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_fault_record_equipment` FOREIGN KEY (`equipment_id`)
        REFERENCES `equipment` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_fault_record_reporter` FOREIGN KEY (`reporter_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_fault_record_level`
        CHECK (`fault_level` IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT `chk_fault_record_status`
        CHECK (`status` IN ('REPORTED', 'ORDER_CREATED', 'PROCESSING', 'RESOLVED', 'CLOSED'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '设备异常记录';

-- 10. 维修工单；fault_record_id 唯一防止同一异常重复创建工单。
CREATE TABLE IF NOT EXISTS `work_order`
(
    `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '维修工单ID',
    `work_order_no`         VARCHAR(32)   NOT NULL COMMENT '工单编号',
    `fault_record_id`       BIGINT        NOT NULL COMMENT '关联异常记录ID',
    `equipment_id`          BIGINT        NOT NULL COMMENT '维修设备ID',
    `priority`              VARCHAR(20)   NOT NULL COMMENT 'LOW/MEDIUM/HIGH/URGENT',
    `status`                VARCHAR(32)   NOT NULL DEFAULT 'PENDING_ASSIGNMENT'
        COMMENT 'PENDING_ASSIGNMENT/PENDING_ACCEPTANCE/PROCESSING/PENDING_VERIFICATION/COMPLETED/REJECTED/CANCELLED',
    `maintainer_id`         BIGINT        NULL COMMENT '维修人员ID',
    `due_at`                DATETIME      NULL COMMENT '要求完成时间',
    `assigned_at`           DATETIME      NULL COMMENT '派单时间',
    `accepted_at`           DATETIME      NULL COMMENT '接单时间',
    `solution`              VARCHAR(1000) NULL COMMENT '维修方案',
    `result_description`    VARCHAR(1000) NULL COMMENT '维修结果说明',
    `submitted_at`          DATETIME      NULL COMMENT '提交维修结果时间',
    `verifier_id`           BIGINT        NULL COMMENT '验收人ID',
    `verification_passed`   TINYINT       NULL COMMENT '验收是否通过：0否，1是',
    `verification_remark`   VARCHAR(500)  NULL COMMENT '验收说明',
    `verified_at`           DATETIME      NULL COMMENT '验收时间',
    `completed_at`          DATETIME      NULL COMMENT '工单完成时间',
    `cancelled_at`          DATETIME      NULL COMMENT '取消时间',
    `cancel_reason`         VARCHAR(500)  NULL COMMENT '取消原因',
    `version`               INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_order_no` (`work_order_no`),
    UNIQUE KEY `uk_work_order_fault` (`fault_record_id`),
    KEY `idx_work_order_maintainer_status` (`maintainer_id`, `status`, `created_at`),
    KEY `idx_work_order_status_priority` (`status`, `priority`, `created_at`),
    KEY `idx_work_order_due_status` (`due_at`, `status`),
    KEY `idx_work_order_equipment` (`equipment_id`, `created_at`),
    CONSTRAINT `fk_work_order_fault` FOREIGN KEY (`fault_record_id`)
        REFERENCES `fault_record` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_work_order_equipment` FOREIGN KEY (`equipment_id`)
        REFERENCES `equipment` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_work_order_maintainer` FOREIGN KEY (`maintainer_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_work_order_verifier` FOREIGN KEY (`verifier_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_work_order_priority`
        CHECK (`priority` IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT `chk_work_order_status`
        CHECK (`status` IN (
            'PENDING_ASSIGNMENT', 'PENDING_ACCEPTANCE', 'PROCESSING',
            'PENDING_VERIFICATION', 'COMPLETED', 'REJECTED', 'CANCELLED'
        )),
    CONSTRAINT `chk_work_order_verify_passed`
        CHECK (`verification_passed` IS NULL OR `verification_passed` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '维修工单';

CREATE TABLE IF NOT EXISTS `work_order_operation`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工单操作记录ID',
    `work_order_id`  BIGINT       NOT NULL COMMENT '工单ID',
    `operation_type` VARCHAR(32)  NOT NULL COMMENT '操作类型',
    `from_status`    VARCHAR(32)  NULL COMMENT '操作前状态',
    `to_status`      VARCHAR(32)  NULL COMMENT '操作后状态',
    `operator_id`    BIGINT       NOT NULL COMMENT '操作人ID',
    `operator_name`  VARCHAR(50)  NOT NULL COMMENT '操作人名称快照',
    `remark`         VARCHAR(500) NULL COMMENT '操作说明',
    `operated_at`    DATETIME     NOT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_order_operation_seed`
        (`work_order_id`, `operation_type`, `operated_at`),
    KEY `idx_work_order_operation_order_time` (`work_order_id`, `operated_at`),
    KEY `idx_work_order_operation_operator_time` (`operator_id`, `operated_at`),
    CONSTRAINT `fk_work_order_operation_order` FOREIGN KEY (`work_order_id`)
        REFERENCES `work_order` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_work_order_operation_user` FOREIGN KEY (`operator_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_work_order_operation_type`
        CHECK (`operation_type` IN (
            'CREATE', 'ASSIGN', 'REASSIGN', 'ACCEPT', 'SUBMIT_RESULT',
            'VERIFY_PASS', 'VERIFY_REJECT', 'REWORK', 'CANCEL'
        )),
    CONSTRAINT `chk_work_order_operation_from_status`
        CHECK (`from_status` IS NULL OR `from_status` IN (
            'PENDING_ASSIGNMENT', 'PENDING_ACCEPTANCE', 'PROCESSING',
            'PENDING_VERIFICATION', 'COMPLETED', 'REJECTED', 'CANCELLED'
        )),
    CONSTRAINT `chk_work_order_operation_to_status`
        CHECK (`to_status` IS NULL OR `to_status` IN (
            'PENDING_ASSIGNMENT', 'PENDING_ACCEPTANCE', 'PROCESSING',
            'PENDING_VERIFICATION', 'COMPLETED', 'REJECTED', 'CANCELLED'
        ))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '工单状态流转记录';

-- 11. 站内通知；RabbitMQ/WebSocket 负责传输，最终通知记录落在此表。
CREATE TABLE IF NOT EXISTS `notification`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `recipient_id`     BIGINT       NOT NULL COMMENT '接收用户ID',
    `type`             VARCHAR(40)  NOT NULL COMMENT '通知类型',
    `title`            VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content`          VARCHAR(500) NOT NULL COMMENT '通知内容',
    `related_biz_type` VARCHAR(30)  NULL COMMENT '关联业务类型',
    `related_biz_id`   BIGINT       NULL COMMENT '关联业务ID',
    `is_read`          TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0否，1是',
    `read_at`          DATETIME     NULL COMMENT '阅读时间',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_biz`
        (`recipient_id`, `type`, `related_biz_type`, `related_biz_id`),
    KEY `idx_notification_user_read_time` (`recipient_id`, `is_read`, `created_at`),
    CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `chk_notification_type`
        CHECK (`type` IN (
            'WORK_ORDER_ASSIGNED', 'URGENT_FAULT',
            'VERIFICATION_PASSED', 'VERIFICATION_REJECTED'
        )),
    CONSTRAINT `chk_notification_read` CHECK (`is_read` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户站内通知';

-- 12. MinIO 文件元数据和业务附件关系；文件本体仍存放在 MinIO。
CREATE TABLE IF NOT EXISTS `file_object`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `bucket_name`   VARCHAR(63)  NOT NULL COMMENT 'MinIO桶名称',
    `object_key`    VARCHAR(500) NOT NULL COMMENT 'MinIO对象Key',
    `url`           VARCHAR(800) NOT NULL COMMENT '文件访问地址',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `content_type`  VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
    `size_bytes`    BIGINT       NOT NULL COMMENT '文件大小（字节）',
    `uploader_id`   BIGINT       NULL COMMENT '上传用户ID',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_object_key` (`object_key`),
    KEY `idx_file_object_uploader_time` (`uploader_id`, `created_at`),
    CONSTRAINT `fk_file_object_uploader` FOREIGN KEY (`uploader_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT `chk_file_object_size` CHECK (`size_bytes` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'MinIO文件元数据';

CREATE TABLE IF NOT EXISTS `biz_attachment`
(
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '附件关系ID',
    `file_id`    BIGINT      NOT NULL COMMENT '文件ID',
    `biz_type`   VARCHAR(20) NOT NULL COMMENT 'INSPECTION/FAULT/REPAIR',
    `biz_id`     BIGINT      NOT NULL COMMENT '业务记录ID',
    `sort_no`    INT         NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_attachment` (`biz_type`, `biz_id`, `file_id`),
    KEY `idx_biz_attachment_file` (`file_id`),
    CONSTRAINT `fk_biz_attachment_file` FOREIGN KEY (`file_id`)
        REFERENCES `file_object` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `chk_biz_attachment_type`
        CHECK (`biz_type` IN ('INSPECTION', 'FAULT', 'REPAIR'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '业务图片附件关系';

-- 13. AOP 操作日志；request_params 写入前必须脱敏密码、Token等敏感信息。
CREATE TABLE IF NOT EXISTS `sys_operation_log`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
    `trace_id`       VARCHAR(64)  NULL COMMENT '请求追踪ID',
    `operator_id`    BIGINT       NULL COMMENT '操作用户ID',
    `operator_name`  VARCHAR(50)  NULL COMMENT '操作用户名称快照',
    `operation_name` VARCHAR(100) NOT NULL COMMENT '操作名称',
    `business_type`  VARCHAR(50)  NOT NULL COMMENT '业务类型',
    `request_method` VARCHAR(10)  NULL COMMENT 'HTTP方法',
    `request_uri`    VARCHAR(255) NULL COMMENT '请求路径',
    `java_method`    VARCHAR(500) NULL COMMENT 'Java方法签名',
    `request_params` TEXT         NULL COMMENT '脱敏后的请求参数',
    `success`        TINYINT      NOT NULL COMMENT '是否成功：0否，1是',
    `error_message`  VARCHAR(1000) NULL COMMENT '失败信息',
    `duration_ms`    BIGINT       NOT NULL DEFAULT 0 COMMENT '执行耗时毫秒',
    `ip_address`     VARCHAR(64)  NULL COMMENT '客户端IP',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_operation_log_trace` (`trace_id`),
    KEY `idx_operation_log_operator_time` (`operator_id`, `created_at`),
    KEY `idx_operation_log_business_time` (`business_type`, `created_at`),
    KEY `idx_operation_log_success_time` (`success`, `created_at`),
    CONSTRAINT `fk_operation_log_operator` FOREIGN KEY (`operator_id`)
        REFERENCES `sys_user` (`id`) ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT `chk_operation_log_success` CHECK (`success` IN (0, 1)),
    CONSTRAINT `chk_operation_log_duration` CHECK (`duration_ms` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AOP业务操作日志';

-- 数据统计不单独建表：仪表盘、趋势和分布数据直接从 equipment、inspection_task、
-- fault_record、work_order 按日期及状态聚合，避免维护重复统计数据。
