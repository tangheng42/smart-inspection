package com.zhixunyun.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InspectionRecordVO {
    private Long id;
    private Long taskId;
    private String taskNo;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long inspectorId;
    private String inspectorName;
    private Boolean normal;
    private LocalDateTime completedAt;
}
