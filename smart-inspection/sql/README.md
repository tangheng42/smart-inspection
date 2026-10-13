# 智巡云数据库脚本

推荐执行方式：

1. 只需要完整表结构：执行 `002_create_core_business_tables.sql`。
2. 还需要演示数据：再执行 `003_seed_demo_data.sql`。

`002_create_core_business_tables.sql` 已包含全部 21 张表，可以单独分享和执行。
`001_create_sys_user.sql` 仅保留为早期“登录模块最小建表脚本”，不再是完整初始化的必需步骤。

三个脚本均不会删除已有表或数据，可以重复执行。演示数据通过唯一编码避免重复插入。

## 表结构分组

- 用户认证：`sys_user`
- 基础资料：`area`、`equipment_type`、`equipment`、`inspection_item`
- 巡检计划：`inspection_plan`、`inspection_plan_schedule`、`inspection_plan_equipment`、`inspection_plan_item`
- 巡检任务：`inspection_task`、`inspection_task_equipment`、`inspection_task_item`
- 巡检结果：`inspection_record`、`inspection_record_item`
- 异常与工单：`fault_record`、`work_order`、`work_order_operation`
- 支撑能力：`notification`、`file_object`、`biz_attachment`、`sys_operation_log`

共 21 张表（包含已有的 `sys_user`）。数据统计直接聚合业务表，不额外保存重复的统计结果。

## 演示账号

脚本会在账号不存在时创建以下账号，新建账号的密码均为 `123456`：

- `admin`：系统管理员
- `inspector01`：巡检员张巡检
- `inspector02`：巡检员王巡检
- `maintainer01`：维修人员李维修
- `maintainer02`：维修人员赵维修

如果同名账号已经存在，脚本不会覆盖其密码或资料。

## 演示业务数据

种子数据包含：

- 4 个区域、4 种设备类型、7 台设备、9 个巡检项目
- 4 个巡检计划、7 个巡检任务、6 条巡检记录
- 4 条异常记录和 4 张维修工单
- 待接单、处理中、待验收、已完成等工单状态
- 工单操作记录、站内通知和 AOP 操作日志样例

`file_object` 和 `biz_attachment` 暂时不插入虚假图片。后续接入 MinIO 并真正上传文件后，再保存对象 Key、URL 和业务关联。
