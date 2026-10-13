package com.zhixunyun.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CurrentUserResponseData", description = "当前登录用户信息")
public class CurrentUserVO {

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "admin")
    private String username;

    @Schema(description = "用户展示名称", example = "系统管理员")
    private String displayName;

    @Schema(description = "角色编码", example = "ADMIN",
            allowableValues = {"ADMIN", "INSPECTOR", "MAINTAINER"})
    private String roleCode;
}
