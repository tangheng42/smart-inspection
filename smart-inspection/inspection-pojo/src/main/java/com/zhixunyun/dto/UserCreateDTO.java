package com.zhixunyun.dto;

import com.zhixunyun.enumeration.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    @NotNull(message = "角色不能为空")
    private RoleCode roleCode;
}
