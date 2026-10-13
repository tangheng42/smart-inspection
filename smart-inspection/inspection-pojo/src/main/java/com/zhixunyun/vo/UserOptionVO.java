package com.zhixunyun.vo;

import com.zhixunyun.enumeration.RoleCode;
import lombok.Data;

@Data
public class UserOptionVO {

    private Long id;

    private String displayName;

    private RoleCode roleCode;
}
