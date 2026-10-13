package com.zhixunyun.vo;

import java.time.LocalDate;

import lombok.Data;

@Data
public class InspectionPlanConflictVO {

    private Long planId;

    private String frequencyType;// 频率类型

    private LocalDate startDate;

    private LocalDate endDate;

}
