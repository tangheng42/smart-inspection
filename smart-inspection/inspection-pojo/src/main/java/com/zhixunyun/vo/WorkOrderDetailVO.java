package com.zhixunyun.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderDetailVO extends WorkOrderVO {
    private String areaName;
    private String faultType;
    private String faultDescription;
    private List<String> faultImageUrls;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private String solution;
    private String resultDescription;
    private List<String> resultImageUrls;
    private LocalDateTime submittedAt;
    private Long verifierId;
    private String verifierName;
    private Boolean verificationPassed;
    private String verificationRemark;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime updatedAt;
}
