package com.zhixunyun.event;

import java.time.LocalDateTime;

public record NotificationEvent(
        String eventId,
        Long recipientId,
        String type,
        String title,
        String content,
        String relatedBizType,
        Long relatedBizId,
        LocalDateTime occurredAt) {
}
