package com.zhixunyun.vo;

import com.zhixunyun.enumeration.InspectionResultType;
import lombok.Data;

@Data
public class InspectionItemOptionVO {

    private Long id;
    private String code;
    private String name;
    private InspectionResultType resultType;
    private String unit;
    private Boolean required;
}
