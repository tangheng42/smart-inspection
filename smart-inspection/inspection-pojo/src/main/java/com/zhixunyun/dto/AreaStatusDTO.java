package com.zhixunyun.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AreaStatusDTO {

    @NotNull(message = "状态不能为空")
    private Boolean enabled;
}
