package com.zhixunyun.rabbitmq;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zhixunyun.event.NotificationEvent;
import com.zhixunyun.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MQ 模式下消费通知事件并按接收人推送 WebSocket。
 * eventId 只在本进程内保留 10 分钟，减少短时间重复投递的重复推送；
 * 进程重启或多实例之间不共享去重状态。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "zhixunyun.rabbitmq", name = "enabled", havingValue = "true")
public class NotificationEventConsumer {

    private static final long DEDUP_SECONDS = 600;
    private final NotificationWebSocketHandler webSocketHandler;
    private final Map<String, Instant> processed = new ConcurrentHashMap<>();

    /** 消费失败交由监听容器的重试配置处理；本类不持久化消费状态。 */
    @RabbitListener(queues = "${zhixunyun.rabbitmq.queue}")
    public void consume(NotificationEvent event) {
        Instant now = Instant.now();
        processed.entrySet().removeIf(entry ->
                entry.getValue().plusSeconds(DEDUP_SECONDS).isBefore(now));
        if (processed.putIfAbsent(event.eventId(), now) != null) {
            return;
        }
        webSocketHandler.send(event);
    }
}
