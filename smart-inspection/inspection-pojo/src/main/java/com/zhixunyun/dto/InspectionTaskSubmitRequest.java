package com.zhixunyun.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class InspectionTaskSubmitRequest {


    /**
     * 各设备巡检结果
     */
    @NotEmpty(message = "巡检结果不能为空")
    @Valid
    private List<EquipmentInspectionResultDTO> equipmentResults;


}
