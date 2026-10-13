package com.zhixunyun.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EquipmentTypeVO {

    private Long id;
    private String code;
    private String name;
    private Boolean enabled;

    // 该类型下面的设备数量
    private Integer equipmentCount;

    // 该类型配置的巡检项目数量
    private Integer inspectionItemCount;

    private LocalDateTime createdAt;
}
