package com.zhixunyun.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InspectionItemResultDTO {


    @NotNull(message = "巡检项目不能为空")
    private Long inspectionItemId;


    /** 数值或布尔文本格式按任务生成时保存的项目类型校验。 */
    @NotBlank(message = "巡检结果值不能为空")
    @Size(max = 500, message = "巡检结果值不能超过500个字符")
    private String resultValue;


    @NotNull(message = "是否正常不能为空")
    private Boolean normal;


    @Size(max = 500, message = "巡检项目备注不能超过500个字符")
    private String remark;

}
