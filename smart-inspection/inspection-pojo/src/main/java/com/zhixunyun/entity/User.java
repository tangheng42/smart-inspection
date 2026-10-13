package com.zhixunyun.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class User {
    private Long id;

    private String username;

    private String passwordHash;

    private String displayName;

    private String roleCode;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
   }
