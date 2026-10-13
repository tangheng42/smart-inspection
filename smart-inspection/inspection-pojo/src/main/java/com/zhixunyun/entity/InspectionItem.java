package com.zhixunyun.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InspectionItem {

    private Long id;
    private String code;
    private String name;
    private Long equipmentTypeId;
    private String checkMethod;
    private String standardDescription;
    private String resultType;
    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer required;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
