package com.zhixunyun.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.zhixunyun.entity.InspectionPlan;
import com.zhixunyun.entity.InspectionTask;
import com.zhixunyun.entity.InspectionTaskEquipment;
import com.zhixunyun.entity.InspectionTaskItem;
import com.zhixunyun.entity.User;
import com.zhixunyun.mapper.InspectionPlanMapper;
import com.zhixunyun.mapper.InspectionTaskMapper;
import com.zhixunyun.mapper.UserMapper;
import com.zhixunyun.service.InspectionTaskGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 按巡检计划生成每日任务及设备、项目快照，并推进下次生成时间。
 * 每个计划单独事务处理，避免一个计划生成失败回滚其他计划。
 */
@Service
@RequiredArgsConstructor
public class InspectionTaskGenerationServiceImpl
        implements InspectionTaskGenerationService {

    private final InspectionPlanMapper planMapper;
    private final InspectionTaskMapper taskMapper;
    private final UserMapper userMapper;

    /**
     * 锁定计划行后重新判断到期条件，在同一事务内生成任务和快照并推进生成游标。
     * 已有任务先按计划及日期跳过；数据库唯一索引 uk_inspection_task_plan_date
     * 是并发或重复调度时的最终防重保障。
     */
    @Override
    @Transactional
    public boolean generateOnePlan(Long planId, LocalDateTime now) {
        InspectionPlan plan = planMapper.getByIdForUpdate(planId);
        if (plan == null || !Integer.valueOf(1).equals(plan.getEnabled())
                || plan.getNextGenerateAt() == null
                || plan.getNextGenerateAt().isAfter(now)) {
            return false;
        }

        LocalDateTime scheduledAt = plan.getNextGenerateAt();
        LocalDate taskDate = scheduledAt.toLocalDate();
        List<Integer> scheduleDays = planMapper.listScheduleDaysByPlanId(planId);
        LocalDateTime nextGenerateAt = calculateNext(plan, scheduledAt, scheduleDays);

        if (taskMapper.existsByPlanAndDate(planId, taskDate) == 0) {
            createTask(plan, scheduledAt);
        }

        planMapper.updateNextGenerateAt(planId, scheduledAt, nextGenerateAt);
        return true;
    }

    /** 仅将已过截止时间、仍为待执行的任务标记逾期。 */
    @Override
    @Transactional
    public int markOverdue(LocalDateTime now) {
        return taskMapper.markOverdue(now);
    }

    /**
     * 复制生成时的负责人、设备和巡检项目配置，历史任务不会随档案修改而漂移。
     * 主任务与两类快照的插入属于同一事务。
     */
    private void createTask(InspectionPlan plan, LocalDateTime scheduledAt) {
        User inspector = userMapper.getById(plan.getInspectorId());
        if (inspector == null || inspector.getDisplayName() == null) {
            throw new IllegalStateException("巡检计划负责人不存在，planId=" + plan.getId());
        }

        List<InspectionTaskEquipment> equipment =
                taskMapper.listEquipmentSnapshots(plan.getId());
        List<InspectionTaskItem> items = taskMapper.listItemSnapshots(plan.getId());
        if (equipment.isEmpty() || items.isEmpty()) {
            throw new IllegalStateException("巡检计划缺少设备或巡检项目，planId=" + plan.getId());
        }

        InspectionTask task = new InspectionTask();
        task.setTaskNo(buildTaskNo(scheduledAt.toLocalDate()));
        task.setPlanId(plan.getId());
        task.setPlanNameSnapshot(plan.getName());
        task.setInspectorId(plan.getInspectorId());
        task.setInspectorNameSnapshot(inspector.getDisplayName());
        task.setTaskDate(scheduledAt.toLocalDate());
        task.setScheduledAt(scheduledAt);
        task.setDeadlineAt(scheduledAt.plusHours(plan.getDeadlineHours()));
        task.setStatus("PENDING");
        task.setEquipmentCount(equipment.size());

        try {
            taskMapper.insertGeneratedTask(task);
        } catch (DuplicateKeyException ex) {
            if (taskMapper.existsByPlanAndDate(
                    plan.getId(), scheduledAt.toLocalDate()) == 1) {
                return;
            }
            throw ex;
        }

        equipment.forEach(row -> row.setTaskId(task.getId()));
        items.forEach(row -> row.setTaskId(task.getId()));
        taskMapper.batchInsertGeneratedEquipment(equipment);
        taskMapper.batchInsertGeneratedItems(items);
    }

    private LocalDateTime calculateNext(
            InspectionPlan plan,
            LocalDateTime current,
            List<Integer> scheduleDays) {
        LocalDate date = current.toLocalDate().plusDays(1);
        LocalDate end = plan.getEndDate();
        for (int i = 0; i < 370; i++, date = date.plusDays(1)) {
            if (end != null && date.isAfter(end)) {
                return null;
            }
            if (matches(plan.getFrequencyType(), date, scheduleDays)) {
                return LocalDateTime.of(date, plan.getExecutionTime());
            }
        }
        return null;
    }

    private boolean matches(
            String frequencyType,
            LocalDate date,
            List<Integer> scheduleDays) {
        return switch (frequencyType.toUpperCase(Locale.ROOT)) {
            case "DAILY" -> true;
            case "WEEKLY" -> scheduleDays.contains(date.getDayOfWeek().getValue());
            case "MONTHLY" -> scheduleDays.contains(date.getDayOfMonth());
            default -> throw new IllegalStateException(
                    "不支持的巡检周期：" + frequencyType);
        };
    }

    private String buildTaskNo(LocalDate taskDate) {
        String random = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        return "IT" + taskDate.toString().replace("-", "") + random;
    }
}
