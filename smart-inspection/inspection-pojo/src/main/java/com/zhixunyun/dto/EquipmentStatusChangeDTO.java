package com.zhixunyun.dto;

import com.zhixunyun.enumeration.EquipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentStatusChangeDTO {

    @NotNull(message = "设备状态不能为空")
    private EquipmentStatus status;

    @Size(max = 255, message = "状态变更原因长度不能超过255个字符")
    private String reason;
}
