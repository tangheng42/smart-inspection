package com.zhixunyun.vo;

import com.zhixunyun.enumeration.FaultLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FaultDistributionItemVO {
    private FaultLevel faultLevel;
    private Long count;
    private Double ratio;
}
