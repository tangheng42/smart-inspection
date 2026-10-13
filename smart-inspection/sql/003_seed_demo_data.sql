-- 智巡云演示数据
-- 新增的演示账号密码均为：123456
-- 只插入不存在的数据，不删除或覆盖用户已有记录；脚本可重复执行。

USE `smart_inspection`;
SET NAMES utf8mb4;

START TRANSACTION;

-- BCrypt(123456)，已有同名用户的密码不会被覆盖。
SET @demo_password_hash = '$2a$10$lvXE1vyKzavZJVl15RFkzuOTnXhBJOXXPahqaT5V7.7mqQGqLgxXO';

INSERT IGNORE INTO `sys_user`
    (`username`, `password_hash`, `display_name`, `role_code`, `enabled`, `created_at`, `updated_at`)
VALUES
    ('admin',        @demo_password_hash, '系统管理员', 'ADMIN',      1, '2026-08-01 08:00:00', '2026-08-01 08:00:00'),
    ('inspector01',  @demo_password_hash, '张巡检',     'INSPECTOR',  1, '2026-08-01 08:05:00', '2026-08-01 08:05:00'),
    ('inspector02',  @demo_password_hash, '王巡检',     'INSPECTOR',  1, '2026-08-01 08:10:00', '2026-08-01 08:10:00'),
    ('maintainer01', @demo_password_hash, '李维修',     'MAINTAINER', 1, '2026-08-01 08:15:00', '2026-08-01 08:15:00'),
    ('maintainer02', @demo_password_hash, '赵维修',     'MAINTAINER', 1, '2026-08-01 08:20:00', '2026-08-01 08:20:00');

SET @admin_id        = (SELECT `id` FROM `sys_user` WHERE `username` = 'admin');
SET @inspector01_id  = (SELECT `id` FROM `sys_user` WHERE `username` = 'inspector01');
SET @inspector02_id  = (SELECT `id` FROM `sys_user` WHERE `username` = 'inspector02');
SET @maintainer01_id = (SELECT `id` FROM `sys_user` WHERE `username` = 'maintainer01');
SET @maintainer02_id = (SELECT `id` FROM `sys_user` WHERE `username` = 'maintainer02');

-- 区域
INSERT IGNORE INTO `area` (`code`, `name`, `description`, `enabled`, `created_at`)
VALUES
    ('AREA-A1', 'A栋一层机房',   'A栋一层弱电及UPS机房', 1, '2026-08-01 09:00:00'),
    ('AREA-A2', 'A栋二层配电室', 'A栋二层低压配电区域',  1, '2026-08-01 09:05:00'),
    ('AREA-B1', 'B栋空调机房',   'B栋中央空调设备区域',  1, '2026-08-01 09:10:00'),
    ('AREA-C1', 'C栋消防泵房',   'C栋消防供水设备区域',  1, '2026-08-01 09:15:00');

SET @area_a1_id = (SELECT `id` FROM `area` WHERE `code` = 'AREA-A1');
SET @area_a2_id = (SELECT `id` FROM `area` WHERE `code` = 'AREA-A2');
SET @area_b1_id = (SELECT `id` FROM `area` WHERE `code` = 'AREA-B1');
SET @area_c1_id = (SELECT `id` FROM `area` WHERE `code` = 'AREA-C1');

-- 设备类型
INSERT IGNORE INTO `equipment_type` (`code`, `name`, `description`, `enabled`, `created_at`)
VALUES
    ('UPS',             'UPS电源',  '不间断电源设备',       1, '2026-08-01 09:30:00'),
    ('AIR_CONDITIONER', '精密空调', '机房温湿度控制设备',   1, '2026-08-01 09:35:00'),
    ('FIRE_PUMP',       '消防泵',   '消防供水与稳压设备',   1, '2026-08-01 09:40:00'),
    ('SWITCHGEAR',      '配电柜',   '低压配电与开关控制柜', 1, '2026-08-01 09:45:00');

SET @type_ups_id  = (SELECT `id` FROM `equipment_type` WHERE `code` = 'UPS');
SET @type_ac_id   = (SELECT `id` FROM `equipment_type` WHERE `code` = 'AIR_CONDITIONER');
SET @type_fire_id = (SELECT `id` FROM `equipment_type` WHERE `code` = 'FIRE_PUMP');
SET @type_sw_id   = (SELECT `id` FROM `equipment_type` WHERE `code` = 'SWITCHGEAR');

-- 设备台账：覆盖正常、故障、维修中、停用四种状态。
INSERT IGNORE INTO `equipment`
    (`code`, `name`, `equipment_type_id`, `area_id`, `model`, `manufacturer`,
     `serial_number`, `install_date`, `status`, `last_inspection_at`, `remark`, `created_at`)
VALUES
    ('UPS-A1-001',  'A1机房1号UPS',   @type_ups_id,  @area_a1_id, 'SPM10K',   '示例电源科技', 'SN-UPS-2026001',  '2026-01-15', 'NORMAL',    '2026-08-27 10:12:00', '核心机房主用UPS',   '2026-08-02 09:00:00'),
    ('UPS-A1-002',  'A1机房2号UPS',   @type_ups_id,  @area_a1_id, 'SPM10K',   '示例电源科技', 'SN-UPS-2026002',  '2026-01-15', 'FAULT',     '2026-08-27 10:15:00', '输出电压偏低待维修', '2026-08-02 09:05:00'),
    ('UPS-A1-003',  'A1机房备用UPS',  @type_ups_id,  @area_a1_id, 'SPM6K',    '示例电源科技', 'SN-UPS-2026003',  '2025-11-20', 'DISABLED',  NULL,                  '已停用备用设备',     '2026-08-02 09:10:00'),
    ('AC-B1-001',   'B1机房1号精密空调', @type_ac_id, @area_b1_id, 'CRAC-30', '示例制冷设备', 'SN-AC-2026001',   '2025-12-10', 'NORMAL',    '2026-08-28 11:12:00', '主用精密空调',       '2026-08-02 09:15:00'),
    ('AC-B1-002',   'B1机房2号精密空调', @type_ac_id, @area_b1_id, 'CRAC-30', '示例制冷设备', 'SN-AC-2026002',   '2025-12-10', 'IN_REPAIR', '2026-08-28 11:15:00', '压缩机高温维修中',   '2026-08-02 09:20:00'),
    ('FIRE-C1-001', 'C1消防稳压泵',   @type_fire_id, @area_c1_id, 'FP-18.5',  '示例消防设备', 'SN-FIRE-2026001', '2025-10-08', 'IN_REPAIR', '2026-08-15 09:18:00', '压力不足待验收',     '2026-08-02 09:25:00'),
    ('SW-A2-001',   'A2低压配电柜1号', @type_sw_id,  @area_a2_id, 'GGD-01',   '示例电气设备', 'SN-SW-2026001',   '2025-09-16', 'NORMAL',    '2026-08-20 15:18:00', '已完成温升故障维修', '2026-08-02 09:30:00');

SET @eq_ups1_id = (SELECT `id` FROM `equipment` WHERE `code` = 'UPS-A1-001');
SET @eq_ups2_id = (SELECT `id` FROM `equipment` WHERE `code` = 'UPS-A1-002');
SET @eq_ac1_id  = (SELECT `id` FROM `equipment` WHERE `code` = 'AC-B1-001');
SET @eq_ac2_id  = (SELECT `id` FROM `equipment` WHERE `code` = 'AC-B1-002');
SET @eq_fire_id = (SELECT `id` FROM `equipment` WHERE `code` = 'FIRE-C1-001');
SET @eq_sw_id   = (SELECT `id` FROM `equipment` WHERE `code` = 'SW-A2-001');

-- 巡检项目模板
INSERT IGNORE INTO `inspection_item`
    (`code`, `name`, `equipment_type_id`, `check_method`, `standard_description`,
     `result_type`, `unit`, `min_value`, `max_value`, `is_required`, `enabled`, `created_at`)
VALUES
    ('UPS-VOLTAGE',     '输出电压',     @type_ups_id,  '读取设备显示屏输出电压', '输出电压应在210V至230V之间', 'NUMBER',  'V',   210, 230, 1, 1, '2026-08-03 09:00:00'),
    ('UPS-BATTERY',     '电池外观',     @type_ups_id,  '检查电池是否鼓包或漏液',   '电池无鼓包、漏液和腐蚀',     'BOOLEAN', NULL,  NULL, NULL, 1, 1, '2026-08-03 09:05:00'),
    ('UPS-TEMP',        '环境温度',     @type_ups_id,  '读取机房温度计',           '环境温度应在15℃至30℃之间',  'NUMBER',  '℃',   15,  30,  1, 1, '2026-08-03 09:10:00'),
    ('AC-OUTLET-TEMP',  '出风温度',     @type_ac_id,   '测量空调出风口温度',       '出风温度应在16℃至26℃之间',  'NUMBER',  '℃',   16,  26,  1, 1, '2026-08-03 09:15:00'),
    ('AC-NOISE',        '运行异响',     @type_ac_id,   '现场听辨压缩机和风机声音', '设备运行无异常噪音',         'BOOLEAN', NULL,  NULL, NULL, 1, 1, '2026-08-03 09:20:00'),
    ('FIRE-PRESSURE',   '管网压力',     @type_fire_id, '读取压力表数值',           '管网压力应在0.40MPa至0.80MPa之间', 'NUMBER', 'MPa', 0.4, 0.8, 1, 1, '2026-08-03 09:25:00'),
    ('FIRE-LEAK',       '管路渗漏',     @type_fire_id, '检查泵体和管路接口',       '泵体和管路接口无渗漏',       'BOOLEAN', NULL,  NULL, NULL, 1, 1, '2026-08-03 09:30:00'),
    ('SW-TEMP',         '母排温度',     @type_sw_id,   '使用红外测温仪测量母排',   '母排温度不得超过80℃',        'NUMBER',  '℃',   NULL, 80,  1, 1, '2026-08-03 09:35:00'),
    ('SW-INDICATOR',    '指示灯状态',   @type_sw_id,   '检查面板指示灯',           '指示灯显示正常且无告警',     'BOOLEAN', NULL,  NULL, NULL, 1, 1, '2026-08-03 09:40:00');

-- 巡检计划
INSERT IGNORE INTO `inspection_plan`
    (`name`, `inspector_id`, `frequency_type`, `execution_time`, `start_date`, `end_date`,
     `deadline_hours`, `enabled`, `next_generate_at`, `remark`, `created_at`)
VALUES
    ('A1机房UPS日常巡检',   @inspector01_id, 'DAILY',   '09:00:00', '2026-08-01', NULL, 4,  1, '2026-08-30 09:00:00', '每日检查两台在用UPS', '2026-08-04 09:00:00'),
    ('B1精密空调例行巡检',  @inspector02_id, 'WEEKLY',  '10:00:00', '2026-08-01', NULL, 8,  1, '2026-08-31 10:00:00', '每周一三五执行',       '2026-08-04 09:05:00'),
    ('C1消防泵月度巡检',    @inspector01_id, 'MONTHLY', '09:00:00', '2026-08-01', NULL, 12, 1, '2026-09-01 09:00:00', '每月1日和15日执行',    '2026-08-04 09:10:00'),
    ('A2配电柜历史巡检计划', @inspector02_id, 'DAILY',   '15:00:00', '2026-08-01', '2026-08-20', 4, 0, NULL, '演示已停用计划', '2026-08-04 09:15:00');

SET @plan_ups_id  = (SELECT `id` FROM `inspection_plan` WHERE `name` = 'A1机房UPS日常巡检');
SET @plan_ac_id   = (SELECT `id` FROM `inspection_plan` WHERE `name` = 'B1精密空调例行巡检');
SET @plan_fire_id = (SELECT `id` FROM `inspection_plan` WHERE `name` = 'C1消防泵月度巡检');
SET @plan_sw_id   = (SELECT `id` FROM `inspection_plan` WHERE `name` = 'A2配电柜历史巡检计划');

INSERT IGNORE INTO `inspection_plan_schedule` (`plan_id`, `day_value`)
VALUES
    (@plan_ac_id, 1), (@plan_ac_id, 3), (@plan_ac_id, 5),
    (@plan_fire_id, 1), (@plan_fire_id, 15);

INSERT IGNORE INTO `inspection_plan_equipment` (`plan_id`, `equipment_id`)
VALUES
    (@plan_ups_id, @eq_ups1_id), (@plan_ups_id, @eq_ups2_id),
    (@plan_ac_id, @eq_ac1_id),   (@plan_ac_id, @eq_ac2_id),
    (@plan_fire_id, @eq_fire_id),
    (@plan_sw_id, @eq_sw_id);

INSERT IGNORE INTO `inspection_plan_item` (`plan_id`, `inspection_item_id`, `sort_no`)
SELECT @plan_ups_id, `id`,
       CASE `code` WHEN 'UPS-VOLTAGE' THEN 1 WHEN 'UPS-BATTERY' THEN 2 ELSE 3 END
FROM `inspection_item` WHERE `code` IN ('UPS-VOLTAGE', 'UPS-BATTERY', 'UPS-TEMP');

INSERT IGNORE INTO `inspection_plan_item` (`plan_id`, `inspection_item_id`, `sort_no`)
SELECT @plan_ac_id, `id`, CASE `code` WHEN 'AC-OUTLET-TEMP' THEN 1 ELSE 2 END
FROM `inspection_item` WHERE `code` IN ('AC-OUTLET-TEMP', 'AC-NOISE');

INSERT IGNORE INTO `inspection_plan_item` (`plan_id`, `inspection_item_id`, `sort_no`)
SELECT @plan_fire_id, `id`, CASE `code` WHEN 'FIRE-PRESSURE' THEN 1 ELSE 2 END
FROM `inspection_item` WHERE `code` IN ('FIRE-PRESSURE', 'FIRE-LEAK');

INSERT IGNORE INTO `inspection_plan_item` (`plan_id`, `inspection_item_id`, `sort_no`)
SELECT @plan_sw_id, `id`, CASE `code` WHEN 'SW-TEMP' THEN 1 ELSE 2 END
FROM `inspection_item` WHERE `code` IN ('SW-TEMP', 'SW-INDICATOR');

-- 巡检任务：包含待巡检、已完成、已逾期、已取消样例。
INSERT IGNORE INTO `inspection_task`
    (`task_no`, `plan_id`, `plan_name_snapshot`, `inspector_id`, `inspector_name_snapshot`,
     `task_date`, `scheduled_at`, `deadline_at`, `status`, `equipment_count`,
     `completed_equipment_count`, `started_at`, `completed_at`, `cancel_reason`, `created_at`)
VALUES
    ('IT202608290001', @plan_ups_id,  'A1机房UPS日常巡检',    @inspector01_id, '张巡检', '2026-08-29', '2026-08-29 09:00:00', '2026-08-29 13:00:00', 'PENDING',   2, 0, NULL,                  NULL,                  NULL,               '2026-08-29 00:05:00'),
    ('IT202608270001', @plan_ups_id,  'A1机房UPS日常巡检',    @inspector01_id, '张巡检', '2026-08-27', '2026-08-27 09:00:00', '2026-08-27 13:00:00', 'COMPLETED', 2, 2, '2026-08-27 10:00:00', '2026-08-27 10:15:00', NULL,               '2026-08-27 00:05:00'),
    ('IT202608280001', @plan_ac_id,   'B1精密空调例行巡检',   @inspector02_id, '王巡检', '2026-08-28', '2026-08-28 10:00:00', '2026-08-28 18:00:00', 'COMPLETED', 2, 2, '2026-08-28 11:00:00', '2026-08-28 11:15:00', NULL,               '2026-08-28 00:05:00'),
    ('IT202608150001', @plan_fire_id, 'C1消防泵月度巡检',     @inspector01_id, '张巡检', '2026-08-15', '2026-08-15 09:00:00', '2026-08-15 21:00:00', 'COMPLETED', 1, 1, '2026-08-15 09:10:00', '2026-08-15 09:18:00', NULL,               '2026-08-15 00:05:00'),
    ('IT202608200001', @plan_sw_id,   'A2配电柜历史巡检计划', @inspector02_id, '王巡检', '2026-08-20', '2026-08-20 15:00:00', '2026-08-20 19:00:00', 'COMPLETED', 1, 1, '2026-08-20 15:10:00', '2026-08-20 15:18:00', NULL,               '2026-08-20 00:05:00'),
    ('IT202608250001', @plan_ups_id,  'A1机房UPS日常巡检',    @inspector01_id, '张巡检', '2026-08-25', '2026-08-25 09:00:00', '2026-08-25 13:00:00', 'OVERDUE',   2, 0, NULL,                  NULL,                  NULL,               '2026-08-25 00:05:00'),
    ('IT202608260001', @plan_ac_id,   'B1精密空调例行巡检',   @inspector02_id, '王巡检', '2026-08-26', '2026-08-26 10:00:00', '2026-08-26 18:00:00', 'CANCELLED', 2, 0, NULL,                  NULL,                  '机房临时封闭',     '2026-08-26 00:05:00');

-- 根据计划关系生成任务设备和任务项目快照。
INSERT IGNORE INTO `inspection_task_equipment`
    (`task_id`, `equipment_id`, `equipment_code_snapshot`, `equipment_name_snapshot`,
     `equipment_type_id`, `area_id`, `area_name_snapshot`, `equipment_status_snapshot`,
     `completion_status`, `sort_no`, `completed_at`, `created_at`)
SELECT
    t.`id`, e.`id`, e.`code`, e.`name`, e.`equipment_type_id`, e.`area_id`, a.`name`,
    CASE WHEN t.`status` = 'PENDING' THEN e.`status` ELSE 'NORMAL' END,
    CASE WHEN t.`status` = 'COMPLETED' THEN 'COMPLETED' ELSE 'PENDING' END,
    ROW_NUMBER() OVER (PARTITION BY t.`id` ORDER BY e.`code`),
    CASE WHEN t.`status` = 'COMPLETED' THEN t.`completed_at` ELSE NULL END,
    t.`created_at`
FROM `inspection_task` t
JOIN `inspection_plan_equipment` pe ON pe.`plan_id` = t.`plan_id`
JOIN `equipment` e ON e.`id` = pe.`equipment_id`
JOIN `area` a ON a.`id` = e.`area_id`
WHERE t.`task_no` IN (
    'IT202608290001', 'IT202608270001', 'IT202608280001', 'IT202608150001',
    'IT202608200001', 'IT202608250001', 'IT202608260001'
);

INSERT IGNORE INTO `inspection_task_item`
    (`task_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`,
     `check_method_snapshot`, `standard_description_snapshot`, `result_type_snapshot`,
     `unit_snapshot`, `min_value_snapshot`, `max_value_snapshot`,
     `is_required_snapshot`, `sort_no`, `created_at`)
SELECT
    t.`id`, i.`id`, i.`code`, i.`name`, i.`check_method`, i.`standard_description`,
    i.`result_type`, i.`unit`, i.`min_value`, i.`max_value`, i.`is_required`,
    pi.`sort_no`, t.`created_at`
FROM `inspection_task` t
JOIN `inspection_plan_item` pi ON pi.`plan_id` = t.`plan_id`
JOIN `inspection_item` i ON i.`id` = pi.`inspection_item_id`
WHERE t.`task_no` IN (
    'IT202608290001', 'IT202608270001', 'IT202608280001', 'IT202608150001',
    'IT202608200001', 'IT202608250001', 'IT202608260001'
);

SET @task_ups_done  = (SELECT `id` FROM `inspection_task` WHERE `task_no` = 'IT202608270001');
SET @task_ac_done   = (SELECT `id` FROM `inspection_task` WHERE `task_no` = 'IT202608280001');
SET @task_fire_done = (SELECT `id` FROM `inspection_task` WHERE `task_no` = 'IT202608150001');
SET @task_sw_done   = (SELECT `id` FROM `inspection_task` WHERE `task_no` = 'IT202608200001');

SET @te_ups1 = (SELECT `id` FROM `inspection_task_equipment` WHERE `task_id` = @task_ups_done AND `equipment_id` = @eq_ups1_id);
SET @te_ups2 = (SELECT `id` FROM `inspection_task_equipment` WHERE `task_id` = @task_ups_done AND `equipment_id` = @eq_ups2_id);
SET @te_ac1  = (SELECT `id` FROM `inspection_task_equipment` WHERE `task_id` = @task_ac_done AND `equipment_id` = @eq_ac1_id);
SET @te_ac2  = (SELECT `id` FROM `inspection_task_equipment` WHERE `task_id` = @task_ac_done AND `equipment_id` = @eq_ac2_id);
SET @te_fire = (SELECT `id` FROM `inspection_task_equipment` WHERE `task_id` = @task_fire_done AND `equipment_id` = @eq_fire_id);
SET @te_sw   = (SELECT `id` FROM `inspection_task_equipment` WHERE `task_id` = @task_sw_done AND `equipment_id` = @eq_sw_id);

-- 巡检记录
INSERT IGNORE INTO `inspection_record`
    (`task_id`, `task_equipment_id`, `equipment_id`, `inspector_id`, `normal`, `remark`, `completed_at`, `created_at`)
VALUES
    (@task_ups_done,  @te_ups1, @eq_ups1_id, @inspector01_id, 1, '各项指标正常',             '2026-08-27 10:12:00', '2026-08-27 10:12:00'),
    (@task_ups_done,  @te_ups2, @eq_ups2_id, @inspector01_id, 0, '输出电压明显低于标准范围', '2026-08-27 10:15:00', '2026-08-27 10:15:00'),
    (@task_ac_done,   @te_ac1,  @eq_ac1_id,  @inspector02_id, 1, '制冷运行正常',             '2026-08-28 11:12:00', '2026-08-28 11:12:00'),
    (@task_ac_done,   @te_ac2,  @eq_ac2_id,  @inspector02_id, 0, '出风温度偏高',             '2026-08-28 11:15:00', '2026-08-28 11:15:00'),
    (@task_fire_done, @te_fire, @eq_fire_id, @inspector01_id, 0, '管网压力低于标准值',       '2026-08-15 09:18:00', '2026-08-15 09:18:00'),
    (@task_sw_done,   @te_sw,   @eq_sw_id,   @inspector02_id, 0, '母排温升异常',             '2026-08-20 15:18:00', '2026-08-20 15:18:00');

SET @record_ups1 = (SELECT `id` FROM `inspection_record` WHERE `task_id` = @task_ups_done AND `equipment_id` = @eq_ups1_id);
SET @record_ups2 = (SELECT `id` FROM `inspection_record` WHERE `task_id` = @task_ups_done AND `equipment_id` = @eq_ups2_id);
SET @record_ac1  = (SELECT `id` FROM `inspection_record` WHERE `task_id` = @task_ac_done AND `equipment_id` = @eq_ac1_id);
SET @record_ac2  = (SELECT `id` FROM `inspection_record` WHERE `task_id` = @task_ac_done AND `equipment_id` = @eq_ac2_id);
SET @record_fire = (SELECT `id` FROM `inspection_record` WHERE `task_id` = @task_fire_done AND `equipment_id` = @eq_fire_id);
SET @record_sw   = (SELECT `id` FROM `inspection_record` WHERE `task_id` = @task_sw_done AND `equipment_id` = @eq_sw_id);

-- 巡检项目结果快照。
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`,
     `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ups1, `id`, `code`, `name`, `standard_description`, `result_type`, '220.5', `unit`, 1, NULL, '2026-08-27 10:12:00'
FROM `inspection_item` WHERE `code` = 'UPS-VOLTAGE';
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ups1, `id`, `code`, `name`, `standard_description`, `result_type`, 'true', `unit`, 1, NULL, '2026-08-27 10:12:00'
FROM `inspection_item` WHERE `code` = 'UPS-BATTERY';
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ups1, `id`, `code`, `name`, `standard_description`, `result_type`, '24.5', `unit`, 1, NULL, '2026-08-27 10:12:00'
FROM `inspection_item` WHERE `code` = 'UPS-TEMP';

INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ups2, `id`, `code`, `name`, `standard_description`, `result_type`, '198.2', `unit`, 0, '持续低于210V', '2026-08-27 10:15:00'
FROM `inspection_item` WHERE `code` = 'UPS-VOLTAGE';
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ups2, `id`, `code`, `name`, `standard_description`, `result_type`, 'true', `unit`, 1, NULL, '2026-08-27 10:15:00'
FROM `inspection_item` WHERE `code` = 'UPS-BATTERY';
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ups2, `id`, `code`, `name`, `standard_description`, `result_type`, '25.0', `unit`, 1, NULL, '2026-08-27 10:15:00'
FROM `inspection_item` WHERE `code` = 'UPS-TEMP';

INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ac1, `id`, `code`, `name`, `standard_description`, `result_type`, '22.0', `unit`, 1, NULL, '2026-08-28 11:12:00'
FROM `inspection_item` WHERE `code` = 'AC-OUTLET-TEMP';
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ac1, `id`, `code`, `name`, `standard_description`, `result_type`, 'true', `unit`, 1, NULL, '2026-08-28 11:12:00'
FROM `inspection_item` WHERE `code` = 'AC-NOISE';

INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ac2, `id`, `code`, `name`, `standard_description`, `result_type`, '29.5', `unit`, 0, '压缩机高温导致制冷不足', '2026-08-28 11:15:00'
FROM `inspection_item` WHERE `code` = 'AC-OUTLET-TEMP';
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_ac2, `id`, `code`, `name`, `standard_description`, `result_type`, 'true', `unit`, 1, NULL, '2026-08-28 11:15:00'
FROM `inspection_item` WHERE `code` = 'AC-NOISE';

INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_fire, `id`, `code`, `name`, `standard_description`, `result_type`, '0.32', `unit`, 0, '低于0.40MPa', '2026-08-15 09:18:00'
FROM `inspection_item` WHERE `code` = 'FIRE-PRESSURE';
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_fire, `id`, `code`, `name`, `standard_description`, `result_type`, 'true', `unit`, 1, NULL, '2026-08-15 09:18:00'
FROM `inspection_item` WHERE `code` = 'FIRE-LEAK';

INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_sw, `id`, `code`, `name`, `standard_description`, `result_type`, '92.0', `unit`, 0, 'B相母排温度异常', '2026-08-20 15:18:00'
FROM `inspection_item` WHERE `code` = 'SW-TEMP';
INSERT IGNORE INTO `inspection_record_item`
    (`inspection_record_id`, `inspection_item_id`, `item_code_snapshot`, `item_name_snapshot`, `standard_description_snapshot`, `result_type_snapshot`, `result_value`, `unit_snapshot`, `normal`, `remark`, `created_at`)
SELECT @record_sw, `id`, `code`, `name`, `standard_description`, `result_type`, 'true', `unit`, 1, NULL, '2026-08-20 15:18:00'
FROM `inspection_item` WHERE `code` = 'SW-INDICATOR';

-- 异常记录
INSERT IGNORE INTO `fault_record`
    (`fault_no`, `inspection_task_id`, `inspection_record_id`, `equipment_id`, `reporter_id`,
     `fault_type`, `fault_level`, `description`, `status`, `reported_at`, `resolved_at`, `closed_at`, `created_at`)
VALUES
    ('FR202608270001', @task_ups_done,  @record_ups2, @eq_ups2_id, @inspector01_id, '电气故障', 'HIGH',   'UPS输出电压持续低于标准范围', 'ORDER_CREATED', '2026-08-27 10:16:00', NULL,                  NULL,                  '2026-08-27 10:16:00'),
    ('FR202608280001', @task_ac_done,   @record_ac2,  @eq_ac2_id,  @inspector02_id, '制冷故障', 'URGENT', '精密空调出风温度持续偏高，存在机房过热风险', 'PROCESSING', '2026-08-28 11:16:00', NULL, NULL, '2026-08-28 11:16:00'),
    ('FR202608150001', @task_fire_done, @record_fire, @eq_fire_id, @inspector01_id, '压力异常', 'MEDIUM', '消防稳压泵管网压力低于标准值', 'PROCESSING', '2026-08-15 09:20:00', NULL, NULL, '2026-08-15 09:20:00'),
    ('FR202608200001', @task_sw_done,   @record_sw,   @eq_sw_id,   @inspector02_id, '温升异常', 'LOW',    '配电柜B相母排温度达到92℃', 'CLOSED', '2026-08-20 15:20:00', '2026-08-20 17:30:00', '2026-08-20 18:00:00', '2026-08-20 15:20:00');

SET @fault_ups  = (SELECT `id` FROM `fault_record` WHERE `fault_no` = 'FR202608270001');
SET @fault_ac   = (SELECT `id` FROM `fault_record` WHERE `fault_no` = 'FR202608280001');
SET @fault_fire = (SELECT `id` FROM `fault_record` WHERE `fault_no` = 'FR202608150001');
SET @fault_sw   = (SELECT `id` FROM `fault_record` WHERE `fault_no` = 'FR202608200001');

-- 工单样例覆盖待接单、处理中、待验收、已完成四个关键状态。
INSERT IGNORE INTO `work_order`
    (`work_order_no`, `fault_record_id`, `equipment_id`, `priority`, `status`, `maintainer_id`,
     `due_at`, `assigned_at`, `accepted_at`, `solution`, `result_description`, `submitted_at`,
     `verifier_id`, `verification_passed`, `verification_remark`, `verified_at`, `completed_at`, `created_at`)
VALUES
    ('WO202608270001', @fault_ups,  @eq_ups2_id, 'HIGH',   'PENDING_ACCEPTANCE',  @maintainer01_id, '2026-08-29 18:00:00', '2026-08-27 10:30:00', NULL,                  NULL,                     NULL,                       NULL,                  NULL,      NULL, NULL,                 NULL,                  NULL,                  '2026-08-27 10:16:00'),
    ('WO202608280001', @fault_ac,   @eq_ac2_id,  'URGENT', 'PROCESSING',          @maintainer02_id, '2026-08-29 18:00:00', '2026-08-28 11:20:00', '2026-08-28 11:30:00', NULL,                     NULL,                       NULL,                  NULL,      NULL, NULL,                 NULL,                  NULL,                  '2026-08-28 11:16:00'),
    ('WO202608150001', @fault_fire, @eq_fire_id, 'MEDIUM', 'PENDING_VERIFICATION', @maintainer01_id, '2026-08-16 18:00:00', '2026-08-15 09:30:00', '2026-08-15 10:00:00', '调整稳压泵压力控制器', '压力恢复至0.52MPa，试运行正常', '2026-08-15 12:30:00', NULL,      NULL, NULL,                 NULL,                  NULL,                  '2026-08-15 09:20:00'),
    ('WO202608200001', @fault_sw,   @eq_sw_id,   'LOW',    'COMPLETED',           @maintainer02_id, '2026-08-21 18:00:00', '2026-08-20 15:30:00', '2026-08-20 15:40:00', '紧固B相母排连接并清洁触点', '维修后温度稳定在56℃',         '2026-08-20 17:30:00', @admin_id, 1, '复测温度正常，验收通过', '2026-08-20 18:00:00', '2026-08-20 18:00:00', '2026-08-20 15:20:00');

SET @wo_ups  = (SELECT `id` FROM `work_order` WHERE `work_order_no` = 'WO202608270001');
SET @wo_ac   = (SELECT `id` FROM `work_order` WHERE `work_order_no` = 'WO202608280001');
SET @wo_fire = (SELECT `id` FROM `work_order` WHERE `work_order_no` = 'WO202608150001');
SET @wo_sw   = (SELECT `id` FROM `work_order` WHERE `work_order_no` = 'WO202608200001');

-- 工单流转记录
INSERT IGNORE INTO `work_order_operation`
    (`work_order_id`, `operation_type`, `from_status`, `to_status`, `operator_id`, `operator_name`, `remark`, `operated_at`)
VALUES
    (@wo_ups,  'CREATE',        NULL,                   'PENDING_ASSIGNMENT',  @inspector01_id,  '张巡检',     '巡检异常自动创建工单',       '2026-08-27 10:16:00'),
    (@wo_ups,  'ASSIGN',        'PENDING_ASSIGNMENT',   'PENDING_ACCEPTANCE',  @admin_id,        '系统管理员', '派发给李维修',               '2026-08-27 10:30:00'),
    (@wo_ac,   'CREATE',        NULL,                   'PENDING_ASSIGNMENT',  @inspector02_id,  '王巡检',     '紧急异常自动创建工单',       '2026-08-28 11:16:00'),
    (@wo_ac,   'ASSIGN',        'PENDING_ASSIGNMENT',   'PENDING_ACCEPTANCE',  @admin_id,        '系统管理员', '紧急工单优先派发',           '2026-08-28 11:20:00'),
    (@wo_ac,   'ACCEPT',        'PENDING_ACCEPTANCE',   'PROCESSING',          @maintainer02_id, '赵维修',     '已到现场开始检查',           '2026-08-28 11:30:00'),
    (@wo_fire, 'CREATE',        NULL,                   'PENDING_ASSIGNMENT',  @inspector01_id,  '张巡检',     '巡检异常自动创建工单',       '2026-08-15 09:20:00'),
    (@wo_fire, 'ASSIGN',        'PENDING_ASSIGNMENT',   'PENDING_ACCEPTANCE',  @admin_id,        '系统管理员', '派发给李维修',               '2026-08-15 09:30:00'),
    (@wo_fire, 'ACCEPT',        'PENDING_ACCEPTANCE',   'PROCESSING',          @maintainer01_id, '李维修',     '接单并开始处理',             '2026-08-15 10:00:00'),
    (@wo_fire, 'SUBMIT_RESULT', 'PROCESSING',           'PENDING_VERIFICATION', @maintainer01_id, '李维修',     '已恢复压力，提交验收',       '2026-08-15 12:30:00'),
    (@wo_sw,   'CREATE',        NULL,                   'PENDING_ASSIGNMENT',  @inspector02_id,  '王巡检',     '巡检异常自动创建工单',       '2026-08-20 15:20:00'),
    (@wo_sw,   'ASSIGN',        'PENDING_ASSIGNMENT',   'PENDING_ACCEPTANCE',  @admin_id,        '系统管理员', '派发给赵维修',               '2026-08-20 15:30:00'),
    (@wo_sw,   'ACCEPT',        'PENDING_ACCEPTANCE',   'PROCESSING',          @maintainer02_id, '赵维修',     '接单并开始处理',             '2026-08-20 15:40:00'),
    (@wo_sw,   'SUBMIT_RESULT', 'PROCESSING',           'PENDING_VERIFICATION', @maintainer02_id, '赵维修',     '紧固连接并完成测温',         '2026-08-20 17:30:00'),
    (@wo_sw,   'VERIFY_PASS',   'PENDING_VERIFICATION', 'COMPLETED',           @admin_id,        '系统管理员', '现场复测正常，验收通过',     '2026-08-20 18:00:00');

-- 站内通知
INSERT IGNORE INTO `notification`
    (`recipient_id`, `type`, `title`, `content`, `related_biz_type`, `related_biz_id`, `is_read`, `read_at`, `created_at`)
VALUES
    (@maintainer01_id, 'WORK_ORDER_ASSIGNED', '您有一张新的维修工单', '工单WO202608270001已派发给您，请及时处理', 'WORK_ORDER', @wo_ups, 0, NULL, '2026-08-27 10:30:00'),
    (@maintainer02_id, 'WORK_ORDER_ASSIGNED', '紧急维修工单待处理',   '紧急工单WO202608280001已派发给您',       'WORK_ORDER', @wo_ac,  1, '2026-08-28 11:28:00', '2026-08-28 11:20:00'),
    (@admin_id,        'URGENT_FAULT',        '发现紧急设备异常',     'B1机房2号精密空调发生紧急制冷故障',      'FAULT_RECORD', @fault_ac, 0, NULL, '2026-08-28 11:16:00'),
    (@maintainer02_id, 'VERIFICATION_PASSED', '维修工单验收通过',     '工单WO202608200001已验收通过并关闭',      'WORK_ORDER', @wo_sw,  1, '2026-08-20 18:05:00', '2026-08-20 18:00:00');

-- AOP操作日志样例。文件表暂不造假数据，等MinIO真正上传后再落库。
INSERT IGNORE INTO `sys_operation_log`
    (`trace_id`, `operator_id`, `operator_name`, `operation_name`, `business_type`,
     `request_method`, `request_uri`, `java_method`, `request_params`, `success`,
     `error_message`, `duration_ms`, `ip_address`, `created_at`)
VALUES
    ('demo-trace-assign-001', @admin_id, '系统管理员', '派发维修工单', 'WORK_ORDER',
     'POST', CONCAT('/api/work-orders/', @wo_ups, '/assign'),
     'WorkOrderService.assign', '{"maintainerId":"demo","dueAt":"2026-08-29T18:00:00"}',
     1, NULL, 38, '127.0.0.1', '2026-08-27 10:30:00'),
    ('demo-trace-verify-001', @admin_id, '系统管理员', '验收维修工单', 'WORK_ORDER',
     'POST', CONCAT('/api/work-orders/', @wo_sw, '/verify'),
     'WorkOrderService.verify', '{"passed":true,"remark":"现场复测正常"}',
     1, NULL, 52, '127.0.0.1', '2026-08-20 18:00:00');

COMMIT;
