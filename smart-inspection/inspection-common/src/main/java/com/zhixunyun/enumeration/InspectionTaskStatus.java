package com.zhixunyun.enumeration;

/** 任务从待执行进入执行中或取消；执行中可完成，逾期只标记未开始的任务。 */
public enum InspectionTaskStatus {

    PENDING,

    IN_PROGRESS,

    COMPLETED,

    OVERDUE,

    CANCELLED,
}
