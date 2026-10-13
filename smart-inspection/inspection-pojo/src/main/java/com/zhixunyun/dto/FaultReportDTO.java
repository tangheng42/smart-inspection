package com.zhixunyun.dto;

import java.util.List;
import com.zhixunyun.enumeration.FaultLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FaultReportDTO {
    @NotBlank(message = "异常类型不能为空")
    @Size(max = 50, message = "异常类型不能超过50个字符")
    private String faultType;

    @NotNull(message = "异常等级不能为空")
    private FaultLevel faultLevel;

    @NotBlank(message = "异常描述不能为空")
    @Size(max = 1000, message = "异常描述不能超过1000个字符")
    private String description;

    @Size(max = 9, message = "异常图片不能超过9张")
    private List<String> imageUrls;
}
