package com.zhixunyun.vo;

import java.time.LocalDateTime;

import com.zhixunyun.enumeration.EquipmentStatus;
import lombok.Data;

@Data
public class EquipmentVO {

    private Long id;
    private String code;
    private String name;
    private Long equipmentTypeId;
    private String equipmentTypeName;
    private Long areaId;
    private String areaName;
    private EquipmentStatus status;
    private LocalDateTime lastInspectionAt;
}
