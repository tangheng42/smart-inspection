package com.zhixunyun.entity;

import java.time.LocalDateTime;

import com.zhixunyun.enumeration.WorkOrderPriority;
import com.zhixunyun.enumeration.WorkOrderStatus;
import lombok.Data;

/** 故障对应的维修工单；版本号用于状态流转的条件更新。 */
@Data
public class WorkOrder {

    private Long id;
    private String workOrderNo;
    private Long faultRecordId;
    private Long equipmentId;
    private WorkOrderPriority priority;
    private WorkOrderStatus status;

    private Long maintainerId;
    private LocalDateTime dueAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private String solution;
    private String resultDescription;
    private LocalDateTime submittedAt;
    private Long verifierId;
    private Boolean verificationPassed;
    private String verificationRemark;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    /** 每次状态变更递增，阻止基于旧工单数据的并发写入。 */
    private Integer version;
}
