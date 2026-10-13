package com.zhixunyun.vo;

import java.time.LocalDateTime;
import com.zhixunyun.enumeration.NotificationType;
import lombok.Data;

@Data
public class NotificationVO {
    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private Long relatedBizId;
    private String relatedBizType;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
