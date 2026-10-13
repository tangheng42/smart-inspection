package com.zhixunyun.vo;

import java.math.BigDecimal;

import com.zhixunyun.enumeration.InspectionResultType;
import lombok.Data;

@Data
public class TaskInspectionItemVO {

    private Long inspectionItemId;

    private String itemCode;

    private String itemName;

    private String checkMethod;

    private String standardDescription;

    private InspectionResultType resultType;

    private String unit;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    private Boolean required;
}
