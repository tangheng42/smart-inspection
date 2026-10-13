package com.zhixunyun.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.zhixunyun.enumeration.EquipmentStatus;
import lombok.Data;

@Data
public class EquipmentDetailVO {

    private Long id;
    private String code;
    private String name;
    private Long equipmentTypeId;
    private String equipmentTypeName;
    private Long areaId;
    private String areaName;
    private EquipmentStatus status;
    private LocalDateTime lastInspectionAt;
    private String model;
    private String manufacturer;
    private String serialNumber;
    private LocalDate installDate;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
