package com.zhixunyun.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 单台设备的完整巡检提交结果；正常标记须与全部项目结果一致。 */
@Data
public class EquipmentInspectionResultDTO {


    @NotNull(message = "设备不能为空")
    private Long equipmentId;

    @NotNull(message = "设备整体巡检结果不能为空")
    private Boolean normal;

    @Size(max = 500, message = "设备巡检备注不能超过500个字符")
    private String remark;

    @Size(max = 9, message = "设备现场图片不能超过9张")
    private List<String> imageUrls;

    /** 必须覆盖任务快照中的全部项目，不能重复或额外提交项目。 */
    @NotEmpty(message = "巡检项目结果不能为空")
    @Valid
    private List<InspectionItemResultDTO> itemResults;

    /** 仅异常设备需填写故障信息，并据此生成故障记录和维修工单。 */
    @Valid
    private FaultReportDTO fault;


}
