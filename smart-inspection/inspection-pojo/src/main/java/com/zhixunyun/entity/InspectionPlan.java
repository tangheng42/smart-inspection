package com.zhixunyun.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;

/** 巡检任务的周期配置；定时器根据 nextGenerateAt 判断是否需要生成。 */
@Data
public class InspectionPlan {

    private Long id;

    private String name;

    private Long inspectorId;

    private String frequencyType;

    /** 每个匹配周期日期的本地执行时刻。 */
    private LocalTime executionTime;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 从计划执行时刻起计算的完成期限，单位为小时。 */
    private Integer deadlineHours;

    private Integer enabled;

    /** 下次待生成时刻；停用或已无未来执行日期时为 null。 */
    private LocalDateTime nextGenerateAt;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
