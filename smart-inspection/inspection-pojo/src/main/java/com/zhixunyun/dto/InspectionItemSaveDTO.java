package com.zhixunyun.dto;

import java.math.BigDecimal;

import com.zhixunyun.enumeration.InspectionResultType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InspectionItemSaveDTO {

    @NotBlank(message = "巡检项目编码不能为空")
    @Size(max = 40, message = "巡检项目编码长度不能超过40个字符")
    private String code;

    @NotBlank(message = "巡检项目名称不能为空")
    @Size(max = 80, message = "巡检项目名称长度不能超过80个字符")
    private String name;

    @NotNull(message = "适用设备类型不能为空")
    @Positive(message = "设备类型ID必须大于0")
    private Long equipmentTypeId;

    @Size(max = 255, message = "检查方法长度不能超过255个字符")
    private String checkMethod;

    @NotBlank(message = "合格标准不能为空")
    @Size(max = 500, message = "合格标准长度不能超过500个字符")
    private String standardDescription;

    @NotNull(message = "结果类型不能为空")
    private InspectionResultType resultType;

    @Size(max = 20, message = "单位长度不能超过20个字符")
    private String unit;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    @NotNull(message = "是否必填不能为空")
    private Boolean required;
}
