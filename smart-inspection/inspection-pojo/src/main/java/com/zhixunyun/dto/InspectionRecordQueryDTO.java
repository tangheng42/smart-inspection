package com.zhixunyun.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class InspectionRecordQueryDTO {
    @Min(1) private Integer pageNum = 1;
    @Min(1) @Max(100) private Integer pageSize = 10;
    private String taskNo;
    private Long equipmentId;
    private Long inspectorId;
    private Boolean normal;
    private LocalDate completedFrom;
    private LocalDate completedTo;
}
