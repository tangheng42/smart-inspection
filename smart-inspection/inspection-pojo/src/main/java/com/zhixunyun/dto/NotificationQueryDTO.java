package com.zhixunyun.dto;

import com.zhixunyun.enumeration.NotificationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class NotificationQueryDTO {
    @Min(1) private Integer pageNum = 1;
    @Min(1) @Max(100) private Integer pageSize = 10;
    private NotificationType type;
    private Boolean read;
}
