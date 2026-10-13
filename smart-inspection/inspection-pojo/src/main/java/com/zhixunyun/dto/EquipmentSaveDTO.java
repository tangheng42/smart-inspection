package com.zhixunyun.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentSaveDTO {

    @NotBlank(message = "设备编码不能为空")
    @Size(max = 40, message = "设备编码长度不能超过40个字符")
    private String code;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 80, message = "设备名称长度不能超过80个字符")
    private String name;

    @NotNull(message = "设备类型不能为空")
    @Positive(message = "设备类型ID必须大于0")
    private Long equipmentTypeId;

    @NotNull(message = "所属区域不能为空")
    @Positive(message = "区域ID必须大于0")
    private Long areaId;

    @Size(max = 80, message = "设备型号长度不能超过80个字符")
    private String model;

    @Size(max = 80, message = "生产厂商长度不能超过80个字符")
    private String manufacturer;

    @Size(max = 80, message = "设备序列号长度不能超过80个字符")
    private String serialNumber;

    private LocalDate installDate;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
