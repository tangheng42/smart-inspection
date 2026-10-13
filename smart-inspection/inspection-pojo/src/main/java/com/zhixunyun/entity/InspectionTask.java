package com.zhixunyun.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 计划某次执行生成的任务主表；名称和负责人姓名是生成时快照。 */
@Data
public class InspectionTask {
    private Long id;
    private String taskNo;
    private Long planId;
    /** 创建任务时复制计划名称，避免计划改名影响历史任务展示。 */
    private String planNameSnapshot;
    private Long inspectorId;
    /** 创建任务时复制负责人姓名，避免用户改名影响历史展示。 */
    private String inspectorNameSnapshot;
    private LocalDate taskDate;
    private LocalDateTime scheduledAt;
    private LocalDateTime deadlineAt;
    /** 当前任务状态；启动、取消、提交和逾期各自限制允许的旧状态。 */
    private String status;
    private Integer equipmentCount;
}
