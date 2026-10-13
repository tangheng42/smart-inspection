package com.zhixunyun.entity;

import java.math.BigDecimal;
import lombok.Data;

/** 任务生成时复制的巡检项目规则，提交结果按该快照校验类型。 */
@Data
public class InspectionTaskItem {
    private Long taskId;
    private Long inspectionItemId;
    private String itemCodeSnapshot;
    private String itemNameSnapshot;
    private String checkMethodSnapshot;
    private String standardDescriptionSnapshot;
    /** 项目生成时的结果类型，避免后来修改项目配置影响本次任务。 */
    private String resultTypeSnapshot;
    private String unitSnapshot;
    private BigDecimal minValueSnapshot;
    private BigDecimal maxValueSnapshot;
    private Integer requiredSnapshot;
    private Integer sortNo;
}
