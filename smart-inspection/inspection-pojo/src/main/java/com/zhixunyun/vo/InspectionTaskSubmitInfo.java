package com.zhixunyun.vo;

import com.zhixunyun.enumeration.InspectionTaskStatus;
import lombok.Data;

@Data
public class InspectionTaskSubmitInfo {

    private Long id;

    private Long planId;

    private Long inspectorId;

    private String inspectorName;

    private InspectionTaskStatus status;

}
