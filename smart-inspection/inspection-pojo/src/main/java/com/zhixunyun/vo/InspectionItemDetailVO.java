package com.zhixunyun.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.zhixunyun.enumeration.InspectionResultType;
import lombok.Data;

@Data
public class InspectionItemDetailVO {

    private Long id;
    private String code;
    private String name;
    private Long equipmentTypeId;
    private String equipmentTypeName;
    private String checkMethod;
    private String standardDescription;
    private InspectionResultType resultType;
    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Boolean required;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
