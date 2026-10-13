package com.zhixunyun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AreaCreateDTO {

    @NotBlank(message = "区域编码不能为空")
    @Size(max = 30, message = "区域编码长度不能超过30个字符")
    private String code;

    @NotBlank(message = "区域名称不能为空")
    @Size(max = 50, message = "区域名称长度不能超过50个字符")
    private String name;

    @Size(max = 255, message = "区域说明长度不能超过255个字符")
    private String description;
}
