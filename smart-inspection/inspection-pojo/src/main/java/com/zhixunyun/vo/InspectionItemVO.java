package com.zhixunyun.vo;

import com.zhixunyun.enumeration.InspectionResultType;
import lombok.Data;

@Data
public class InspectionItemVO {

    private Long id;
    private String code;
    private String name;
    private Long equipmentTypeId;
    private String equipmentTypeName;
    private InspectionResultType resultType;
    private Boolean required;
    private Boolean enabled;
}
