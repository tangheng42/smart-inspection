package com.zhixunyun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReasonDTO {
    @NotBlank(message = "原因不能为空")
    @Size(max = 500, message = "原因不能超过500个字符")
    private String reason;
}
