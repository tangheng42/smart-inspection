package com.zhixunyun.dto;

import java.time.LocalDate;

import com.zhixunyun.enumeration.FaultLevel;
import com.zhixunyun.enumeration.FaultStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FaultRecordQueryDTO {
    @Min(1) private Integer pageNum = 1;
    @Min(1) @Max(100) private Integer pageSize = 10;
    private String faultNo;
    private Long equipmentId;
    private FaultLevel faultLevel;
    private FaultStatus status;
    private LocalDate reportedFrom;
    private LocalDate reportedTo;
    private Long reporterId;
    private Long maintainerId;
}
