package com.zhixunyun.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AreaVO {

    private Long id;

    private String code;

    private String name;

    private Boolean enabled;

    private Integer equipmentCount;

    private LocalDateTime createdAt;
}
