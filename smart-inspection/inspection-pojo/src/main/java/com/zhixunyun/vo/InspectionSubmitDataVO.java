package com.zhixunyun.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InspectionSubmitDataVO {
    private Long inspectionTaskId;
    private List<Long> inspectionRecordIds;
    private List<Long> faultRecordIds;
    private List<Long> workOrderIds;
}
