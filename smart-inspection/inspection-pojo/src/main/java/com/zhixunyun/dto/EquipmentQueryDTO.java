package com.zhixunyun.dto;

import com.zhixunyun.enumeration.EquipmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentQueryDTO {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum;

    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize;

    @Size(max = 80, message = "搜索关键词长度不能超过80个字符")
    private String keyword;

    @Positive(message = "设备类型ID必须大于0")
    private Long equipmentTypeId;

    @Positive(message = "区域ID必须大于0")
    private Long areaId;

    private EquipmentStatus status;
}
