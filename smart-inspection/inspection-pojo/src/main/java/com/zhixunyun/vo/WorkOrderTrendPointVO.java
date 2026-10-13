package com.zhixunyun.vo;

import lombok.Data;

@Data
public class WorkOrderTrendPointVO {
    private String period;
    private Long createdCount;
    private Long completedCount;
}
