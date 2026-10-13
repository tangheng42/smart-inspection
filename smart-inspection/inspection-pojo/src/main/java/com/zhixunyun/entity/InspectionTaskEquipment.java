package com.zhixunyun.entity;

import lombok.Data;

/** 任务生成时复制的设备信息，提交时以设备 ID 核对任务范围。 */
@Data
public class InspectionTaskEquipment {
    private Long taskId;
    private Long equipmentId;
    private String equipmentCodeSnapshot;
    private String equipmentNameSnapshot;
    private Long equipmentTypeId;
    private Long areaId;
    private String areaNameSnapshot;
    private String equipmentStatusSnapshot;
    private Integer sortNo;
}
