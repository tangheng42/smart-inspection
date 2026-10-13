package com.zhixunyun.entity;

import lombok.Data;

@Data
public class SysOperationLog {
    private Long id;
    private String traceId;
    private Long operatorId;
    private String operatorName;
    private String operationName;
    private String businessType;
    private String requestMethod;
    private String requestUri;
    private String javaMethod;
    private String requestParams;
    private Integer success;
    private String errorMessage;
    private Long durationMs;
    private String ipAddress;
}
