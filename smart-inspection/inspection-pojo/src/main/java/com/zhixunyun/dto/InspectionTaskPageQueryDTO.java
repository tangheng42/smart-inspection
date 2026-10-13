package com.zhixunyun.dto;

import java.time.LocalDate;

import com.zhixunyun.enumeration.InspectionTaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InspectionTaskPageQueryDTO {

    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    /**
     * 任务编号
     */
    @Size(max = 40)
    private String taskNo;

    /**
     * 巡检员ID
     * ADMIN 可以指定
     * INSPECTOR 传入时忽略
     */
    private Long inspectorId;

    /**
     * PENDING / IN_PROGRESS / COMPLETED / OVERDUE / CANCELLED
     */
    private InspectionTaskStatus status;

    /**
     * 任务日期开始
     */
    private LocalDate taskDateFrom;

    /**
     * 任务日期结束
     */
    private LocalDate taskDateTo;
}
