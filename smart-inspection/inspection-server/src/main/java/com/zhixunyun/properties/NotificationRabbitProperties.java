package com.zhixunyun.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhixunyun.rabbitmq")
public class NotificationRabbitProperties {
    private boolean enabled;
    private String exchange;
    private String queue;
    private String routingKey;
}
