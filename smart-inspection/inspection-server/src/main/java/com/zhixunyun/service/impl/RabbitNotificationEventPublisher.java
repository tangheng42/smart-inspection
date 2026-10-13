package com.zhixunyun.service.impl;

import com.zhixunyun.event.NotificationEvent;
import com.zhixunyun.properties.NotificationRabbitProperties;
import com.zhixunyun.service.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 工单通知落库后，在事务提交后向 Direct Exchange 发布实时推送事件。
 * 发送失败只记录错误，不撤销已提交的业务和通知记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "zhixunyun.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitNotificationEventPublisher implements NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationRabbitProperties properties;

    @Override
    public void publishAfterCommit(NotificationEvent event) {
        DirectNotificationEventPublisher.runAfterCommit(() -> {
            try {
                rabbitTemplate.convertAndSend(
                        properties.getExchange(), properties.getRoutingKey(), event);
            } catch (Exception ex) {
                // 通知记录已落库；MQ故障不能回滚已经成功的工单业务。
                log.error("RabbitMQ通知事件发送失败，eventId={}", event.eventId(), ex);
            }
        });
    }
}
