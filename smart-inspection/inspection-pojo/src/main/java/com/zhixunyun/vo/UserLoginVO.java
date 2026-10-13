package com.zhixunyun.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UserLoginResponseData", description = "登录成功后返回的用户身份和 JWT 信息")
public class UserLoginVO {

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "admin")
    private String username;

    @Schema(description = "用户展示名称", example = "系统管理员")
    private String displayName;

    @Schema(description = "角色编码", example = "ADMIN",
            allowableValues = {"ADMIN", "INSPECTOR", "MAINTAINER"})
    private String roleCode;

    @Schema(description = "JWT；后续请求放入 Authorization 请求头", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token 剩余有效期，单位：秒", example = "7200")
    private Long expiresIn;
}
