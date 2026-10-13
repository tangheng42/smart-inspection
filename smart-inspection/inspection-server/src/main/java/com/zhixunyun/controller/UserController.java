package com.zhixunyun.controller;

import java.util.List;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.config.Knife4jConfig;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.ResetPasswordDTO;
import com.zhixunyun.dto.UserCreateDTO;
import com.zhixunyun.dto.UserDetailVO;
import com.zhixunyun.dto.UserQueryDTO;
import com.zhixunyun.dto.UserStatusDTO;
import com.zhixunyun.dto.UserUpdateDTO;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.UserService;
import com.zhixunyun.vo.CurrentUserVO;
import com.zhixunyun.vo.IdVO;
import com.zhixunyun.vo.UserOptionVO;
import com.zhixunyun.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Slf4j
@Tag(name = "02. 用户管理", description = "当前用户信息与后台用户管理接口")
@SecurityRequirement(name = Knife4jConfig.SECURITY_SCHEME_NAME)
public class UserController {

    @Autowired
    private UserService userService;
    /**
     * 获取当前用户信息
     */
    @Operation(
            summary = "【已实现】获取当前用户信息",
            description = "JWT 拦截器解析 Token 后，将 userId 写入 ThreadLocal；本接口据此查询当前用户。"
    )
    @GetMapping("/me")
    public Result<CurrentUserVO> getCurrentUser() {
        Long userId = BaseContext.getCurrentId();
        CurrentUserVO currentUserVO = userService.getCurrentUser(userId);
        log.info("当前用户信息: {}", currentUserVO);
        return Result.success(currentUserVO);
    }

    /**
     * 分页查询用户
     */
    @GetMapping
    @Operation(summary = "分页查询用户")
    @RequireRole("ADMIN")
    public Result<PageResult<UserVO>> pageUsers(
            @Valid UserQueryDTO userQueryDTO) {

        PageResult<UserVO> pageResult =
                userService.pageUsers(userQueryDTO);

        return Result.success(pageResult);
    }

    /**
     * 新增用户
     */
    @PostMapping
    @Operation(summary = "新增用户")
    public Result<IdVO> createUser(
            @Valid @RequestBody UserCreateDTO userCreateDTO) {

        Long userId = userService.createUser(userCreateDTO);

        return Result.success(new IdVO(userId));
    }

    /**
     * 查询用户下拉选项
     */
    @GetMapping("/options")
    @Operation(summary = "查询用户下拉选项")
    public Result<List<UserOptionVO>> getUserOptions(
            @RequestParam RoleCode roleCode,
            @RequestParam(required = false) String keyword) {

        List<UserOptionVO> list =
                userService.getUserOptions(roleCode, keyword);

        return Result.success(list);
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{userId}")
    @Operation(summary = "查询用户详情")
    public Result<UserDetailVO> getUser(@PathVariable Long userId) {

        UserDetailVO userDetailVO = userService.getUser(userId);

        return Result.success(userDetailVO);
    }

    /**
     * 修改用户基本信息
     */
    @PutMapping("/{userId}")
    public Result<Void> updateUser(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserUpdateDTO dto) {


        userService.updateUser(userId, dto);

        return Result.success();
    }

    /**
     * 修改用户状态
     */
    @PatchMapping("/{userId}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusDTO dto) {

        userService.updateUserStatus(userId, dto);

        return Result.success();
    }

    /**
     * 管理员重置用户密码
     */
    @PostMapping("/{userId}/password/reset")
    @Operation(summary = "管理员重置用户密码")
    public Result<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ResetPasswordDTO dto) {

        userService.resetPassword(userId, dto);

        return Result.success();
    }

}
