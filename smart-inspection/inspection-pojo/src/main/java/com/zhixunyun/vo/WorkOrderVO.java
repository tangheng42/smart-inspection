package com.zhixunyun.vo;

import java.time.LocalDateTime;
import com.zhixunyun.enumeration.WorkOrderPriority;
import com.zhixunyun.enumeration.WorkOrderStatus;
import lombok.Data;

@Data
public class WorkOrderVO {
    private Long id;
    private String workOrderNo;
    private Long faultRecordId;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private WorkOrderPriority priority;
    private WorkOrderStatus status;
    private Long maintainerId;
    private String maintainerName;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
}
