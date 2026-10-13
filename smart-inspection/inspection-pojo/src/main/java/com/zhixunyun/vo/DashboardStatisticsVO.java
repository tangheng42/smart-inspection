package com.zhixunyun.vo;

import lombok.Data;

@Data
public class DashboardStatisticsVO {
    private Long equipmentTotalCount;
    private Long todayPendingInspectionCount;
    private Long todayCompletedInspectionCount;
    private Long abnormalEquipmentCount;
    private Long pendingWorkOrderCount;
    private Long urgentWorkOrderCount;
    private Double workOrderCompletionRate;
}
