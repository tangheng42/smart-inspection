package com.zhixunyun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增、修改设备类型时提交的数据。
 * 两个接口字段完全一致，因此共用一个 DTO，避免重复定义。
 */
@Data
public class EquipmentTypeSaveDTO {

    @NotBlank(message = "设备类型编码不能为空")
    @Size(max = 30, message = "设备类型编码长度不能超过30个字符")
    private String code;

    @NotBlank(message = "设备类型名称不能为空")
    @Size(max = 50, message = "设备类型名称长度不能超过50个字符")
    private String name;

    @Size(max = 255, message = "设备类型说明长度不能超过255个字符")
    private String description;
}
