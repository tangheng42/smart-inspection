package com.zhixunyun.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.zhixunyun.enumeration.InspectionTaskStatus;
import lombok.Data;

@Data
public class InspectionTaskPageVO {

    private Long id;

    /**
     * 任务编号
     */
    private String taskNo;

    /**
     * 来源巡检计划
     */
    private Long planId;

    private String planName;

    /**
     * 巡检员
     */
    private Long inspectorId;

    private String inspectorName;

    /**
     * 任务所属日期
     */
    private LocalDate taskDate;

    /**
     * 截止时间
     */
    private LocalDateTime deadlineAt;

    /**
     * PENDING / IN_PROGRESS / COMPLETED / OVERDUE / CANCELLED
     */
    private InspectionTaskStatus status;

    /**
     * 任务包含多少台设备
     */
    private Integer equipmentCount;

    /**
     * 已完成多少台设备
     */
    private Integer completedEquipmentCount;

    /**
     * 实际开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 实际完成时间
     */
    private LocalDateTime completedAt;
}
