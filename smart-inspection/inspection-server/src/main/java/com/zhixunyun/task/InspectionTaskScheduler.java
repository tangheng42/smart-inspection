package com.zhixunyun.task;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.zhixunyun.mapper.InspectionPlanMapper;
import com.zhixunyun.service.InspectionTaskGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时扫描到期计划和逾期任务。逐个调用事务服务生成任务，
 * 单个计划失败只记录日志，不阻断本轮其他计划。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "zhixunyun.task", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class InspectionTaskScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final InspectionPlanMapper planMapper;
    private final InspectionTaskGenerationService generationService;

    @Scheduled(cron = "${zhixunyun.task.generate-cron:0/30 * * * * ?}")
    public void generateDueTasks() {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        List<Long> planIds = planMapper.listDuePlanIds(now, 50);
        for (Long planId : planIds) {
            try {
                generationService.generateOnePlan(planId, now);
            } catch (Exception ex) {
                log.error("自动生成巡检任务失败，planId={}", planId, ex);
            }
        }
    }

    @Scheduled(cron = "${zhixunyun.task.overdue-cron:0 0/5 * * * ?}")
    public void markOverdueTasks() {
        int affected = generationService.markOverdue(
                LocalDateTime.now(BUSINESS_ZONE));
        if (affected > 0) {
            log.info("已将 {} 个逾期巡检任务标记为 OVERDUE", affected);
        }
    }
}
