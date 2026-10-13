package com.zhixunyun.entity;

import java.time.LocalDateTime;
import com.zhixunyun.enumeration.WorkOrderOperationType;
import com.zhixunyun.enumeration.WorkOrderStatus;
import lombok.Data;

@Data
public class WorkOrderOperation {
    private Long workOrderId;
    private WorkOrderOperationType operationType;
    private WorkOrderStatus fromStatus;
    private WorkOrderStatus toStatus;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime operatedAt;
}
