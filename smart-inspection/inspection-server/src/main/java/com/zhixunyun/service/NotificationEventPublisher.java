package com.zhixunyun.service;

import com.zhixunyun.event.NotificationEvent;

public interface NotificationEventPublisher {
    void publishAfterCommit(NotificationEvent event);
}
