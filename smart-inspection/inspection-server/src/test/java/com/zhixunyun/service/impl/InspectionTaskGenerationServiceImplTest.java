package com.zhixunyun.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.zhixunyun.entity.InspectionPlan;
import com.zhixunyun.entity.InspectionTask;
import com.zhixunyun.entity.InspectionTaskEquipment;
import com.zhixunyun.entity.InspectionTaskItem;
import com.zhixunyun.entity.User;
import com.zhixunyun.mapper.InspectionPlanMapper;
import com.zhixunyun.mapper.InspectionTaskMapper;
import com.zhixunyun.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InspectionTaskGenerationServiceImplTest {

    private final InspectionPlanMapper planMapper = mock(InspectionPlanMapper.class);
    private final InspectionTaskMapper taskMapper = mock(InspectionTaskMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final InspectionTaskGenerationServiceImpl service =
            new InspectionTaskGenerationServiceImpl(planMapper, taskMapper, userMapper);

    @Test
    void shouldGenerateTaskSnapshotsAndAdvanceDailyPlan() {
        LocalDateTime scheduled = LocalDateTime.of(2026, 9, 12, 9, 0);
        InspectionPlan plan = plan("DAILY", scheduled);
        when(planMapper.getByIdForUpdate(1L)).thenReturn(plan);
        when(planMapper.listScheduleDaysByPlanId(1L)).thenReturn(List.of());
        when(taskMapper.existsByPlanAndDate(1L, scheduled.toLocalDate())).thenReturn(0);
        when(userMapper.getById(2L)).thenReturn(user());
        when(taskMapper.listEquipmentSnapshots(1L)).thenReturn(List.of(equipment()));
        when(taskMapper.listItemSnapshots(1L)).thenReturn(List.of(item()));
        doAnswer(invocation -> {
            InspectionTask task = invocation.getArgument(0);
            task.setId(99L);
            return 1;
        }).when(taskMapper).insertGeneratedTask(any());

        boolean generated = service.generateOnePlan(
                1L, scheduled.plusMinutes(1));

        assertThat(generated).isTrue();
        verify(taskMapper).batchInsertGeneratedEquipment(any());
        verify(taskMapper).batchInsertGeneratedItems(any());
        verify(planMapper).updateNextGenerateAt(
                1L, scheduled, scheduled.plusDays(1));
    }

    @Test
    void existingPlanDateShouldNotGenerateDuplicateTask() {
        LocalDateTime scheduled = LocalDateTime.of(2026, 9, 12, 9, 0);
        when(planMapper.getByIdForUpdate(1L)).thenReturn(plan("DAILY", scheduled));
        when(planMapper.listScheduleDaysByPlanId(1L)).thenReturn(List.of());
        when(taskMapper.existsByPlanAndDate(1L, scheduled.toLocalDate())).thenReturn(1);

        service.generateOnePlan(1L, scheduled.plusMinutes(1));

        verify(taskMapper, never()).insertGeneratedTask(any());
        verify(planMapper).updateNextGenerateAt(
                1L, scheduled, scheduled.plusDays(1));
    }

    @Test
    void weeklyPlanShouldAdvanceToConfiguredWeekday() {
        LocalDateTime friday = LocalDateTime.of(2026, 9, 11, 9, 0);
        InspectionPlan plan = plan("WEEKLY", friday);
        when(planMapper.getByIdForUpdate(1L)).thenReturn(plan);
        when(planMapper.listScheduleDaysByPlanId(1L)).thenReturn(List.of(1, 5));
        when(taskMapper.existsByPlanAndDate(eq(1L), any(LocalDate.class))).thenReturn(1);

        service.generateOnePlan(1L, friday.plusMinutes(1));

        verify(planMapper).updateNextGenerateAt(
                1L, friday, LocalDateTime.of(2026, 9, 14, 9, 0));
    }

    private InspectionPlan plan(String frequency, LocalDateTime scheduled) {
        InspectionPlan plan = new InspectionPlan();
        plan.setId(1L);
        plan.setName("测试计划");
        plan.setInspectorId(2L);
        plan.setFrequencyType(frequency);
        plan.setExecutionTime(LocalTime.of(9, 0));
        plan.setStartDate(scheduled.toLocalDate());
        plan.setEndDate(scheduled.toLocalDate().plusMonths(3));
        plan.setDeadlineHours(24);
        plan.setEnabled(1);
        plan.setNextGenerateAt(scheduled);
        return plan;
    }

    private User user() {
        User user = new User();
        user.setId(2L);
        user.setDisplayName("张巡检");
        return user;
    }

    private InspectionTaskEquipment equipment() {
        InspectionTaskEquipment row = new InspectionTaskEquipment();
        row.setEquipmentId(10L);
        return row;
    }

    private InspectionTaskItem item() {
        InspectionTaskItem row = new InspectionTaskItem();
        row.setInspectionItemId(20L);
        return row;
    }
}
