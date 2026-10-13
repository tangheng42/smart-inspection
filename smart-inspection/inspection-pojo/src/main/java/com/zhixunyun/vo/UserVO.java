package com.zhixunyun.vo;

import java.time.LocalDateTime;

import com.zhixunyun.enumeration.RoleCode;
import lombok.Data;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String displayName;

    private RoleCode roleCode;

    private Boolean enabled;

    private LocalDateTime createdAt;
}
