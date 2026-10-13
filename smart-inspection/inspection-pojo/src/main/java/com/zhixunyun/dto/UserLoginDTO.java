package com.zhixunyun.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "UserLoginRequest", description = "用户登录请求参数")
public class UserLoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "登录账号", example = "admin", maxLength = 32,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码", example = "123456", format = "password", maxLength = 64,
            accessMode = Schema.AccessMode.WRITE_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
