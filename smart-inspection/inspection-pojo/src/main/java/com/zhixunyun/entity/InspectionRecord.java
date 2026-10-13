package com.zhixunyun.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InspectionRecord {

    private Long id;

    private Long taskId;

    private Long taskEquipmentId;

    private Long equipmentId;

    private Long inspectorId;

    private Boolean normal;

    private String remark;

    private LocalDateTime completedAt;

}
