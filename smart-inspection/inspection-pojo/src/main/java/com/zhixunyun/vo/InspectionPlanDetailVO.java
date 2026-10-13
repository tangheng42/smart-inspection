package com.zhixunyun.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.zhixunyun.enumeration.FrequencyType;
import lombok.Data;

@Data
public class InspectionPlanDetailVO {

    private Long id;
    private String name;
    private Long inspectorId;
    private String inspectorName;
    private FrequencyType frequencyType;
    private LocalTime executionTime;
    private List<Integer> weekDays;
    private List<Integer> monthDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer deadlineHours;
    private Integer equipmentCount;
    private Integer inspectionItemCount;
    private Boolean enabled;
    private LocalDateTime nextGenerateAt;
    private List<EquipmentOptionVO> equipment;
    private List<InspectionItemOptionVO> inspectionItems;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
