package com.zhixunyun.entity;

import com.zhixunyun.enumeration.InspectionResultType;
import lombok.Data;

@Data
public class InspectionRecordItem {
    private Long inspectionRecordId;
    private Long inspectionItemId;
    private String itemCodeSnapshot;
    private String itemNameSnapshot;
    private String standardDescriptionSnapshot;
    private InspectionResultType resultTypeSnapshot;
    private String resultValue;
    private String unitSnapshot;
    private Boolean normal;
    private String remark;
}
