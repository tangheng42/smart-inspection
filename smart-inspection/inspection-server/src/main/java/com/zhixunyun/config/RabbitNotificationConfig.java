package com.zhixunyun.config;

import com.zhixunyun.properties.NotificationRabbitProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 MQ 时声明持久化的 Direct Exchange、队列及精确匹配的路由键。
 * 当前路由只承载站内通知的 WebSocket 推送事件。
 */
@EnableRabbit
@Configuration
@ConditionalOnProperty(
        prefix = "zhixunyun.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitNotificationConfig {

    @Bean
    public DirectExchange notificationExchange(NotificationRabbitProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue notificationQueue(NotificationRabbitProperties properties) {
        return new Queue(properties.getQueue(), true);
    }

    @Bean
    public Binding notificationBinding(
            DirectExchange notificationExchange,
            Queue notificationQueue,
            NotificationRabbitProperties properties) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange).with(properties.getRoutingKey());
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
