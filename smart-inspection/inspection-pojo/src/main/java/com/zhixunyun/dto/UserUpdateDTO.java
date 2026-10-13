package com.zhixunyun.dto;

import com.zhixunyun.enumeration.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {

    @NotBlank(message = "显示名称不能为空")
    @Size(max = 50, message = "显示名称长度不能超过50个字符")
    String displayName;

    @NotNull(message = "角色不能为空")
    RoleCode roleCode;
}
