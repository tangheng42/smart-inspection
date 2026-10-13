package com.zhixunyun.vo;

import com.zhixunyun.enumeration.FaultLevel;
import lombok.Data;

@Data
public class CountByFaultLevelVO {
    private FaultLevel faultLevel;
    private Long count;
}
