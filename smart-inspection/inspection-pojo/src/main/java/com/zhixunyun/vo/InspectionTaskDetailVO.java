package com.zhixunyun.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.zhixunyun.enumeration.InspectionTaskStatus;
import lombok.Data;

@Data
public class InspectionTaskDetailVO {

    private Long id;

    private String taskNo;

    private Long planId;

    private String planName;

    private Long inspectorId;

    private String inspectorName;

    private LocalDate taskDate;

    private LocalDateTime deadlineAt;

    private InspectionTaskStatus status;

    private Integer equipmentCount;

    private Integer completedEquipmentCount;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private List<TaskEquipmentVO> equipment;

    private String cancelReason;

    private LocalDateTime createdAt;
}
