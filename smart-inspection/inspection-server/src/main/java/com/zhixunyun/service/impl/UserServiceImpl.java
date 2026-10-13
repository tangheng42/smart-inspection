package com.zhixunyun.service.impl;

import com.zhixunyun.annotation.OperationLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.ResetPasswordDTO;
import com.zhixunyun.dto.UserCreateDTO;
import com.zhixunyun.dto.UserDetailVO;
import com.zhixunyun.dto.UserLoginDTO;
import com.zhixunyun.dto.UserQueryDTO;
import com.zhixunyun.dto.UserStatusDTO;
import com.zhixunyun.dto.UserUpdateDTO;
import com.zhixunyun.entity.User;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.UserMapper;
import com.zhixunyun.properties.JwtProperties;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.UserService;
import com.zhixunyun.utils.JwtUtil;
import com.zhixunyun.vo.CurrentUserVO;
import com.zhixunyun.vo.UserLoginVO;
import com.zhixunyun.vo.UserOptionVO;
import com.zhixunyun.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** 用户登录、账号管理与密码重置；登录令牌中携带用户 ID 和角色供请求鉴权。 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private JwtProperties jwtProperties;

    /** 校验 BCrypt 密码及账号启用状态后签发包含身份和角色声明的 JWT。 */
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        User user = userMapper.getByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getEnabled() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("roleCode", user.getRoleCode());

        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims
        );

        UserLoginVO userLoginVO = new UserLoginVO();

        userLoginVO.setUserId(user.getId());
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setDisplayName(user.getDisplayName());
        userLoginVO.setRoleCode(user.getRoleCode());

        userLoginVO.setToken(token);
        userLoginVO.setTokenType("Bearer");
        userLoginVO.setExpiresIn(jwtProperties.getTtl() / 1000);

        return userLoginVO;
    }

    /**
     * 获取当前用户信息
     */
    @Override
    public CurrentUserVO getCurrentUser(Long userId) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setUserId(user.getId());
        currentUserVO.setUsername(user.getUsername());
        currentUserVO.setDisplayName(user.getDisplayName());
        currentUserVO.setRoleCode(user.getRoleCode());
        return currentUserVO;
    }

    /**
     * 分页查询用户
     */
    @Override
    public PageResult<UserVO> pageUsers(UserQueryDTO userQueryDTO) {
        // 1. 开启分页
        PageHelper.startPage(
                userQueryDTO.getPageNum(),
                userQueryDTO.getPageSize()
        );

        // 2. 查询数据库
        List<User> userList = userMapper.pageQuery(userQueryDTO);

        // 3. 获取分页信息
        PageInfo<User> pageInfo = new PageInfo<>(userList);

        // 4. Entity 转 VO
        List<UserVO> records = userList.stream()
                .map(user -> {
                    UserVO vo = new UserVO();
                    vo.setId(user.getId());
                    vo.setUsername(user.getUsername());
                    vo.setDisplayName(user.getDisplayName());
                    vo.setRoleCode(RoleCode.valueOf(user.getRoleCode()));
                    vo.setEnabled(user.getEnabled() == 1);
                    vo.setCreatedAt(user.getCreatedAt());
                    return vo;
                })
                .toList();

        // 5. 组装分页结果
        return new PageResult<>(
                userQueryDTO.getPageNum(),
                userQueryDTO.getPageSize(),
                pageInfo.getTotal(),
                records
        );
    }

        /**
         * 新增用户
         */
    @Override
    @OperationLog(value = "新增用户", businessType = "USER")
    public Long createUser(UserCreateDTO userCreateDTO) {
        // 1. 判断用户名是否存在
        User existingUser = userMapper.getByUsername(userCreateDTO.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        // 2. BCrypt加密密码
        String encodedPassword =
                passwordEncoder.encode(userCreateDTO.getPassword());

        // 3. DTO → User
        User user = new User();
        user.setPasswordHash(encodedPassword);
        user.setDisplayName(userCreateDTO.getDisplayName());
        user.setRoleCode(userCreateDTO.getRoleCode().name());
        user.setEnabled(1);
        user.setUsername(userCreateDTO.getUsername());

        // 4. Mapper INSERT
        userMapper.insert(user);

        // 5. 重新查询完整数据
        User savedUser = userMapper.getById(user.getId());

        // 6. User → UserVO
        UserVO userVO = new UserVO();
        userVO.setId(savedUser.getId());
        userVO.setUsername(savedUser.getUsername());
        userVO.setDisplayName(savedUser.getDisplayName());
        userVO.setRoleCode(RoleCode.valueOf(savedUser.getRoleCode()));
        userVO.setEnabled(savedUser.getEnabled() == 1);
        userVO.setCreatedAt(savedUser.getCreatedAt());

        return savedUser.getId();
    }

    /**
     * 查询用户下拉选项
     */
    @Override
    public List<UserOptionVO> getUserOptions(RoleCode roleCode, String keyword) {
        List<User> userList = userMapper.getUserOptions(roleCode, keyword);

        return userList.stream()
                .map(user -> {
                    UserOptionVO vo = new UserOptionVO();
                    vo.setId(user.getId());
                    vo.setDisplayName(user.getDisplayName());
                    vo.setRoleCode(RoleCode.valueOf(user.getRoleCode()));
                    return vo;
                })
                .toList();
    }

    /**
     * 查询用户详情
     */
    @Override
    public UserDetailVO getUser(Long userId) {

        User user = userMapper.getById(userId);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRoleCode(RoleCode.valueOf(user.getRoleCode()));
        vo.setEnabled(user.getEnabled() == 1);
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());

        return vo;
    }

    /**
     * 修改用户基本信息
     */
    @Override
    @OperationLog(value = "修改用户", businessType = "USER")
    public void updateUser(Long userId, UserUpdateDTO dto) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setDisplayName(dto.getDisplayName());
        user.setRoleCode(dto.getRoleCode().name());

        userMapper.updateById(user);
    }

    /**
     * 修改用户状态
     */
    @Override
    @OperationLog(value = "启停用户", businessType = "USER")
    public void updateUserStatus(Long userId, UserStatusDTO dto) {

        // 1. 判断目标用户是否存在
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 获取当前登录用户ID
        Long currentUserId = BaseContext.getCurrentId();

        // 3. 不能禁用自己
        if (currentUserId.equals(userId)
                && Boolean.FALSE.equals(dto.getEnabled())) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF);
        }

        // 4. 修改状态
        user.setEnabled(dto.getEnabled() ? 1 : 0);

        // 5. 更新数据库
        userMapper.updateStatus(user);
    }

    /**
     * 管理员重置用户密码
     */
    @Override
    @OperationLog(value = "重置用户密码", businessType = "USER")
    public void resetPassword(Long userId, ResetPasswordDTO dto) {

        // 判断用户是否存在
        User user = userMapper.getById(userId);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 新密码不能直接存数据库，必须重新 BCrypt
        String passwordHash = passwordEncoder.encode(dto.getNewPassword());

        // 更新密码
        userMapper.updatePassword(userId, passwordHash);
    }
}
