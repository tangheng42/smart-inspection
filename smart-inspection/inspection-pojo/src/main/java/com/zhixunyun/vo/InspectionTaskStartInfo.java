package com.zhixunyun.vo;

import com.zhixunyun.enumeration.InspectionTaskStatus;
import lombok.Data;

@Data
public class InspectionTaskStartInfo {

    private Long id;

    private Long inspectorId;

    private InspectionTaskStatus status;
}
