package com.zhixunyun.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Knife4jConfig {

    public static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智巡云 API 接口文档")
                        .version("v0.1.0")
                        .description("""
                                智能设备巡检与故障工单平台后端接口。

                                登录接口无需 Token；其他接口在 Knife4j 的“Authorize”中填写登录返回的 Token。
                                认证格式为 `Authorization: Bearer <token>`。

                                统一响应中 `code = 0` 表示成功，非 0 表示业务失败。
                                """)
                        .license(new License().name("仅供智巡云项目学习使用")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地开发环境")
                ))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("填写登录接口返回的 token，Knife4j 会自动添加 Bearer 前缀")
                ));
    }

}
