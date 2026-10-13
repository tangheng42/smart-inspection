package com.zhixunyun.entity;

import lombok.Data;

@Data
public class BizAttachment {
    private Long fileId;
    private String bizType;
    private Long bizId;
    private Integer sortNo;
}
