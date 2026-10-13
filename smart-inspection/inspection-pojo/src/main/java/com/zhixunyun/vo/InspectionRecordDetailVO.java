package com.zhixunyun.vo;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionRecordDetailVO extends InspectionRecordVO {
    private String areaName;
    private String remark;
    private List<String> imageUrls;
    private List<InspectionItemResultVO> itemResults;
    private Long faultRecordId;
}
