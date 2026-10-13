package com.zhixunyun.entity;

import com.zhixunyun.enumeration.NotificationType;
import lombok.Data;

@Data
public class Notification {
    private Long recipientId;
    private NotificationType type;
    private String title;
    private String content;
    private String relatedBizType;
    private Long relatedBizId;
}
