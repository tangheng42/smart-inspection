package com.zhixunyun.dto;

import java.time.LocalDate;

import com.zhixunyun.enumeration.WorkOrderPriority;
import com.zhixunyun.enumeration.WorkOrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class WorkOrderQueryDTO {
    @Min(1) private Integer pageNum = 1;
    @Min(1) @Max(100) private Integer pageSize = 10;
    private String workOrderNo;
    private Long equipmentId;
    private Long maintainerId;
    private WorkOrderPriority priority;
    private WorkOrderStatus status;
    private LocalDate createdFrom;
    private LocalDate createdTo;
}
