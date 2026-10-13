package com.zhixunyun.controller;

import com.zhixunyun.dto.UserLoginDTO;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.UserService;
import com.zhixunyun.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "01. 认证管理", description = "登录与身份认证接口")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 登录
     */
    @Operation(
            summary = "【已实现】用户登录",
            description = "使用账号和密码登录。成功后返回 JWT，后续受保护接口通过 Authorization: Bearer <token> 携带。"
    )
    @PostMapping("/login")
    public Result<UserLoginVO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "登录账号与密码",
                    required = true
            )
            @Valid @RequestBody UserLoginDTO userLoginDTO) {

        log.info("登录：{}", userLoginDTO.getUsername());

        UserLoginVO userLoginVO = userService.login(userLoginDTO);

        return Result.success(userLoginVO);
    }
}
