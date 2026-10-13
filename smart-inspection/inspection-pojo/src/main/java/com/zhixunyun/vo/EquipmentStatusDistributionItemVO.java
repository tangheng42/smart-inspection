package com.zhixunyun.vo;

import com.zhixunyun.enumeration.EquipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EquipmentStatusDistributionItemVO {
    private EquipmentStatus status;
    private Long count;
    private Double ratio;
}
