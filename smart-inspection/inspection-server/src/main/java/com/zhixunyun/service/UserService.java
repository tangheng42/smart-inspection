package com.zhixunyun.service;

import java.util.List;

import com.zhixunyun.dto.ResetPasswordDTO;
import com.zhixunyun.dto.UserCreateDTO;
import com.zhixunyun.dto.UserDetailVO;
import com.zhixunyun.dto.UserLoginDTO;
import com.zhixunyun.dto.UserQueryDTO;
import com.zhixunyun.dto.UserStatusDTO;
import com.zhixunyun.dto.UserUpdateDTO;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.CurrentUserVO;
import com.zhixunyun.vo.UserLoginVO;
import com.zhixunyun.vo.UserOptionVO;
import com.zhixunyun.vo.UserVO;
import jakarta.validation.Valid;

public interface UserService {
    /**
     * 登录
     */
    UserLoginVO login(UserLoginDTO userLoginDTO);

    /**
     * 获取当前用户信息
     */
       CurrentUserVO getCurrentUser(Long userId);

     /**
      * 分页查询用户
      */
    PageResult<UserVO> pageUsers(@Valid UserQueryDTO userQueryDTO);

     /**
      * 新增用户
      */
    Long createUser(@Valid UserCreateDTO userCreateDTO);

     /**
      * 查询用户下拉选项
      */
    List<UserOptionVO> getUserOptions(RoleCode roleCode, String keyword);

     /**
      * 查询用户详情
      */
    UserDetailVO getUser(Long userId);

     /**
      * 修改用户基本信息
      */
    void updateUser(Long userId, @Valid UserUpdateDTO dto);

     /**
      * 修改用户状态
      */
    void updateUserStatus(Long userId, @Valid UserStatusDTO dto);

     /**
      * 管理员重置用户密码
      */
    void resetPassword(Long userId, @Valid ResetPasswordDTO dto);
}
