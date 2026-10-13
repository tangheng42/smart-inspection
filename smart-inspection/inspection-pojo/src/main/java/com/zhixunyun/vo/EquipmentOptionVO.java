package com.zhixunyun.vo;

import lombok.Data;

@Data
public class EquipmentOptionVO {

    private Long id;
    private String code;
    private String name;
    private Long equipmentTypeId;
    private Long areaId;
}
