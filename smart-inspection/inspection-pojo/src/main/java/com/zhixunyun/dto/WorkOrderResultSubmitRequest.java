package com.zhixunyun.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkOrderResultSubmitRequest {

    @NotBlank(message = "维修方案不能为空")
    @Size(max = 1000, message = "维修方案不能超过1000个字符")
    private String solution;

    @NotBlank(message = "维修结果说明不能为空")
    @Size(max = 1000, message = "维修结果说明不能超过1000个字符")
    private String resultDescription;

    @NotEmpty(message = "至少上传一张维修结果图片")
    @Size(max = 9, message = "维修结果图片不能超过9张")
    private List<String> imageUrls;
}
