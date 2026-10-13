package com.zhixunyun.vo;

import com.zhixunyun.enumeration.InspectionResultType;
import lombok.Data;

@Data
public class InspectionItemResultVO {
    private Long inspectionItemId;
    private String itemCode;
    private String itemName;
    private InspectionResultType resultType;
    private String standardDescription;
    private String resultValue;
    private String unit;
    private Boolean normal;
    private String remark;
}
