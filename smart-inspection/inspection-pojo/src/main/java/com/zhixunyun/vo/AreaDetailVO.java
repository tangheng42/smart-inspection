package com.zhixunyun.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AreaDetailVO {

    private Long id;
    private String code;
    private String name;
    private Boolean enabled;
    private Integer equipmentCount;
    private LocalDateTime createdAt;
    private String description;
    private LocalDateTime updatedAt;
}
