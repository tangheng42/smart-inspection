package com.zhixunyun.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EquipmentTypeDetailVO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean enabled;
    private Integer equipmentCount;
    private Integer inspectionItemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
