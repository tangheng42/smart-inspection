package com.zhixunyun.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.zhixunyun.enumeration.FrequencyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InspectionPlanSaveDTO {

    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称长度不能超过100个字符")
    private String name;

    @NotNull(message = "巡检员不能为空")
    @Positive(message = "巡检员ID必须大于0")
    private Long inspectorId;

    @NotNull(message = "巡检周期不能为空")
    private FrequencyType frequencyType;

    @NotNull(message = "执行时间不能为空")
    private LocalTime executionTime;

    /** 每周执行日按 ISO 星期编号 1～7 表示。 */
    private List<@NotNull(message = "每周执行日不能为空")
            @Min(value = 1, message = "每周执行日必须在1到7之间")
            @Max(value = 7, message = "每周执行日必须在1到7之间") Integer> weekDays;

    /** 每月只允许 1～28 日，避免短月无对应日期。 */
    private List<@NotNull(message = "每月执行日不能为空")
            @Min(value = 1, message = "每月执行日必须在1到28之间")
            @Max(value = 28, message = "每月执行日必须在1到28之间") Integer> monthDays;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    /** 从每次计划执行时刻起算的任务期限，单位为小时。 */
    @Min(value = 1, message = "截止小时数不能小于1")
    @Max(value = 168, message = "截止小时数不能超过168")
    private Integer deadlineHours = 24;

    @NotEmpty(message = "至少选择一个设备")
    private List<@NotNull(message = "设备ID不能为空")
            @Positive(message = "设备ID必须大于0") Long> equipmentIds;

    @NotEmpty(message = "至少选择一个巡检项")
    private List<@NotNull(message = "巡检项目ID不能为空")
            @Positive(message = "巡检项目ID必须大于0") Long> inspectionItemIds;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
