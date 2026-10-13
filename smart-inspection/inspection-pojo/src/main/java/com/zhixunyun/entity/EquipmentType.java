package com.zhixunyun.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EquipmentType {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
