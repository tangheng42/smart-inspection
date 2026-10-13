package com.zhixunyun.vo;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.zhixunyun.enumeration.FrequencyType;
import lombok.Data;

@Data
public class InspectionPlanPageVO {

    private Long id;

    private String name;

    private Long inspectorId;

    private String inspectorName;

    private FrequencyType frequencyType;

    private LocalTime executionTime;

    private Integer equipmentCount;

    private Integer inspectionItemCount;

    private Boolean enabled;

    private LocalDateTime nextGenerateAt;
}
