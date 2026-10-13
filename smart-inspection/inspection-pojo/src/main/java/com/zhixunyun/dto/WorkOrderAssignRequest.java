package com.zhixunyun.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Future;
import lombok.Data;

@Data
public class WorkOrderAssignRequest {

    @NotNull(message = "维修人员不能为空")
    private Long maintainerId;

    @NotNull(message = "截止时间不能为空")
    @Future(message = "截止时间必须晚于当前时间")
    private OffsetDateTime dueAt;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
