package com.zhixunyun.service.impl;

import com.zhixunyun.event.NotificationEvent;
import com.zhixunyun.service.NotificationEventPublisher;
import com.zhixunyun.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** MQ 未启用时直接推送 WebSocket；仍等待事务提交，避免推送回滚的数据。 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "zhixunyun.rabbitmq", name = "enabled",
        havingValue = "false", matchIfMissing = true)
public class DirectNotificationEventPublisher implements NotificationEventPublisher {

    private final NotificationWebSocketHandler webSocketHandler;

    @Override
    public void publishAfterCommit(NotificationEvent event) {
        runAfterCommit(() -> webSocketHandler.send(event));
    }

    /** 有事务时注册提交后回调；无事务时立即执行。 */
    static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    });
        } else {
            action.run();
        }
    }
}
