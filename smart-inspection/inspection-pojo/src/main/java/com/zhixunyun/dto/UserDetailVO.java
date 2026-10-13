package com.zhixunyun.dto;

import java.time.LocalDateTime;

import com.zhixunyun.enumeration.RoleCode;
import lombok.Data;

@Data
public class UserDetailVO {

    private Long id;

    private String username;

    private String displayName;

    private RoleCode roleCode;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
