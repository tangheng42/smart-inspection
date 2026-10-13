package com.zhixunyun.vo;

import com.zhixunyun.enumeration.EquipmentStatus;
import lombok.Data;

@Data
public class CountByEquipmentStatusVO {
    private EquipmentStatus status;
    private Long count;
}
