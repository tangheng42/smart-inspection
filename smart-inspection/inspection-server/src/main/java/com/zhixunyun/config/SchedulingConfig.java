package com.zhixunyun.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 按配置开关启用计划生成与逾期扫描任务。 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "zhixunyun.task", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
