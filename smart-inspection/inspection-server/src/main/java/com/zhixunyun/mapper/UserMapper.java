package com.zhixunyun.mapper;

import java.util.List;

import com.zhixunyun.dto.UserQueryDTO;
import com.zhixunyun.entity.User;
import com.zhixunyun.enumeration.RoleCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     */
    @Select("select * from sys_user where username = #{username}")
    User getByUsername(String username);

    /**
     * 根据用户ID查询用户
     */
    @Select("select * from sys_user where id = #{userId}")
    User getById(Long userId);

    /**
     * 分页查询用户
     */
    List<User> pageQuery(UserQueryDTO userQueryDTO);

    /**
     * 插入用户
     */
    void insert(User user);

    /**
     * 查询用户下拉选项
     */
    List<User> getUserOptions(RoleCode roleCode, String keyword);

    /**
     * 更新用户
     */
    @Update("""
    update sys_user
    set display_name = #{displayName},
        role_code = #{roleCode}
    where id = #{id}
    """)
    void updateById(User user);

    /**
     * 更新用户状态
     */
    @Update("""
    update sys_user
    set enabled = #{enabled}
    where id = #{id}
    """)
    void updateStatus(User user);

    /**
     * 更新用户密码
     */
    @Update("""
        update sys_user
        set password_hash = #{passwordHash}
        where id = #{userId}
        """)
    void updatePassword(Long userId, String passwordHash);
}
