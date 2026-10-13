package com.zhixunyun.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FaultRecordDetailVO extends FaultRecordVO {
    private Long inspectionTaskId;
    private String inspectionTaskNo;
    private Long inspectionRecordId;
    private String areaName;
    private String description;
    private List<String> imageUrls;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
}
