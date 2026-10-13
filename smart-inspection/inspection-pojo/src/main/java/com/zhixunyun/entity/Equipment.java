package com.zhixunyun.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Equipment {

    private Long id;
    private String code;
    private String name;
    private Long equipmentTypeId;
    private Long areaId;
    private String model;
    private String manufacturer;
    private String serialNumber;
    private LocalDate installDate;
    private String status;
    private LocalDateTime lastInspectionAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
