package com.zhixunyun.vo;

import java.util.List;

import com.zhixunyun.enumeration.EquipmentStatus;
import lombok.Data;

@Data
public class TaskEquipmentVO {

    private Long equipmentId;

    private String equipmentCode;

    private String equipmentName;

    private String areaName;

    private EquipmentStatus status;

    private List<TaskInspectionItemVO> inspectionItems;
}
