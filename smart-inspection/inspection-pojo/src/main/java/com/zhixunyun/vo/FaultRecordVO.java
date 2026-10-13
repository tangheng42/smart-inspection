package com.zhixunyun.vo;

import java.time.LocalDateTime;
import com.zhixunyun.enumeration.FaultLevel;
import com.zhixunyun.enumeration.FaultStatus;
import lombok.Data;

@Data
public class FaultRecordVO {
    private Long id;
    private String faultNo;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String faultType;
    private FaultLevel faultLevel;
    private FaultStatus status;
    private Long reporterId;
    private String reporterName;
    private LocalDateTime reportedAt;
    private Long workOrderId;
    private String workOrderNo;
}
