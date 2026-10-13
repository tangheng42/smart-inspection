package com.zhixunyun.entity;

import java.time.LocalDateTime;
import com.zhixunyun.enumeration.FaultLevel;
import com.zhixunyun.enumeration.FaultStatus;
import lombok.Data;

@Data
public class FaultRecord {
    private Long id;
    private String faultNo;
    private Long inspectionTaskId;
    private Long inspectionRecordId;
    private Long equipmentId;
    private Long reporterId;
    private String faultType;
    private FaultLevel faultLevel;
    private String description;
    private FaultStatus status;
    private LocalDateTime reportedAt;
}
