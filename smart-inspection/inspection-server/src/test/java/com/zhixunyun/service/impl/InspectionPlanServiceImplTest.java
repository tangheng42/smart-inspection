package com.zhixunyun.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.zhixunyun.dto.InspectionPlanSaveDTO;
import com.zhixunyun.dto.InspectionPlanStatusDTO;
import com.zhixunyun.entity.Equipment;
import com.zhixunyun.entity.InspectionItem;
import com.zhixunyun.entity.InspectionPlan;
import com.zhixunyun.entity.User;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.FrequencyType;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.EquipmentMapper;
import com.zhixunyun.mapper.InspectionItemMapper;
import com.zhixunyun.mapper.InspectionPlanMapper;
import com.zhixunyun.mapper.UserMapper;
import com.zhixunyun.vo.InspectionPlanConflictVO;
import com.zhixunyun.vo.InspectionPlanDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionPlanServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private EquipmentMapper equipmentMapper;

    @Mock
    private InspectionItemMapper inspectionItemMapper;

    @Mock
    private InspectionPlanMapper inspectionPlanMapper;

    @InjectMocks
    private InspectionPlanServiceImpl inspectionPlanService;

    @Test
    void createDailyPlanShouldWriteMainAndRelations() {
        InspectionPlanSaveDTO dto = validDto();
        stubValidReferences(dto, 1L);
        stubNoExistingPlan(dto);
        doAnswer(invocation -> {
            InspectionPlan plan = invocation.getArgument(0);
            plan.setId(99L);
            return 1;
        }).when(inspectionPlanMapper).insert(any(InspectionPlan.class));

        Long id = inspectionPlanService.create(dto);

        assertThat(id).isEqualTo(99L);
        ArgumentCaptor<InspectionPlan> captor =
                ArgumentCaptor.forClass(InspectionPlan.class);
        verify(inspectionPlanMapper).insert(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("测试日计划");
        assertThat(captor.getValue().getRemark()).isNull();
        assertThat(captor.getValue().getDeadlineHours()).isEqualTo(24);
        assertThat(captor.getValue().getNextGenerateAt())
                .isEqualTo(dto.getStartDate().atTime(dto.getExecutionTime()));
        verify(inspectionPlanMapper).batchInsertEquipment(99L, dto.getEquipmentIds());
        verify(inspectionPlanMapper).batchInsertItems(99L, dto.getInspectionItemIds());
        verify(inspectionPlanMapper, never()).batchInsertSchedule(any(), anyList());
    }

    @Test
    void createWeeklyPlanShouldWriteScheduleDays() {
        InspectionPlanSaveDTO dto = validDto();
        dto.setName("每周计划");
        dto.setFrequencyType(FrequencyType.WEEKLY);
        dto.setWeekDays(List.of(1, 3, 5));
        stubValidReferences(dto, 1L);
        stubNoExistingPlan(dto);
        doAnswer(invocation -> {
            InspectionPlan plan = invocation.getArgument(0);
            plan.setId(100L);
            return 1;
        }).when(inspectionPlanMapper).insert(any(InspectionPlan.class));

        inspectionPlanService.create(dto);

        verify(inspectionPlanMapper)
                .batchInsertSchedule(100L, List.of(1, 3, 5));
    }

    @Test
    void duplicateWeekDaysShouldBeRejectedBeforeDatabaseAccess() {
        InspectionPlanSaveDTO dto = validDto();
        dto.setFrequencyType(FrequencyType.WEEKLY);
        dto.setWeekDays(List.of(1, 1));

        assertBusinessError(
                () -> inspectionPlanService.create(dto),
                ErrorCode.INSPECTION_PLAN_SCHEDULE_INVALID
        );
        verify(equipmentMapper, never()).getByIds(anyList());
        verify(inspectionPlanMapper, never()).insert(any());
    }

    @Test
    void expiredPlanShouldBeRejectedBeforeDatabaseAccess() {
        InspectionPlanSaveDTO dto = validDto();
        dto.setStartDate(LocalDate.now().minusDays(3));
        dto.setEndDate(LocalDate.now().minusDays(1));

        assertBusinessError(
                () -> inspectionPlanService.create(dto),
                ErrorCode.INSPECTION_PLAN_NO_FUTURE_EXECUTION
        );
        verify(equipmentMapper, never()).getByIds(anyList());
    }

    @Test
    void equipmentTypesMustBeConsistent() {
        InspectionPlanSaveDTO dto = validDto();
        dto.setEquipmentIds(List.of(1L, 2L));
        when(equipmentMapper.getByIds(dto.getEquipmentIds()))
                .thenReturn(List.of(equipment(1L, 1L), equipment(2L, 2L)));

        assertBusinessError(
                () -> inspectionPlanService.create(dto),
                ErrorCode.INSPECTION_PLAN_EQUIPMENT_TYPE_MISMATCH
        );
    }

    @Test
    void itemTypeMustMatchEquipmentType() {
        InspectionPlanSaveDTO dto = validDto();
        stubValidEquipmentAndInspector(dto);
        when(inspectionItemMapper.getByIds(dto.getInspectionItemIds()))
                .thenReturn(List.of(item(10L, 2L)));

        assertBusinessError(
                () -> inspectionPlanService.create(dto),
                ErrorCode.INSPECTION_PLAN_ITEM_TYPE_MISMATCH
        );
    }

    @Test
    void selectedUserMustBeEnabledInspector() {
        InspectionPlanSaveDTO dto = validDto();
        when(equipmentMapper.getByIds(dto.getEquipmentIds()))
                .thenReturn(List.of(equipment(1L, 1L)));
        User maintainer = inspector();
        maintainer.setRoleCode(RoleCode.MAINTAINER.name());
        when(userMapper.getById(dto.getInspectorId())).thenReturn(maintainer);

        assertBusinessError(
                () -> inspectionPlanService.create(dto),
                ErrorCode.INSPECTION_PLAN_INSPECTOR_UNAVAILABLE
        );
    }

    @Test
    void existingScheduleOnSameEquipmentAndTimeShouldConflict() {
        InspectionPlanSaveDTO dto = validDto();
        stubValidReferences(dto, 1L);
        when(inspectionPlanMapper.getByName(dto.getName().trim())).thenReturn(null);
        InspectionPlanConflictVO conflict = new InspectionPlanConflictVO();
        conflict.setPlanId(7L);
        conflict.setFrequencyType("DAILY");
        conflict.setStartDate(dto.getStartDate());
        conflict.setEndDate(dto.getEndDate());
        when(inspectionPlanMapper.findConflictCandidates(
                anyList(), any(LocalTime.class), any(LocalDate.class),
                nullable(LocalDate.class), nullable(Long.class)))
                .thenReturn(List.of(conflict));
        when(inspectionPlanMapper.findSchedulesByPlanIds(List.of(7L)))
                .thenReturn(List.of());

        assertBusinessError(
                () -> inspectionPlanService.create(dto),
                ErrorCode.INSPECTION_PLAN_CONFLICT
        );
        verify(inspectionPlanMapper, never()).insert(any());
    }

    @Test
    void uniqueIndexRaceShouldBeConvertedToBusinessError() {
        InspectionPlanSaveDTO dto = validDto();
        stubValidReferences(dto, 1L);
        stubNoExistingPlan(dto);
        doThrow(new DuplicateKeyException("duplicate plan name"))
                .when(inspectionPlanMapper).insert(any(InspectionPlan.class));

        assertBusinessError(
                () -> inspectionPlanService.create(dto),
                ErrorCode.INSPECTION_PLAN_NAME_EXISTS
        );
    }

    @Test
    void getByIdShouldAssembleWeeklyScheduleAndRelations() {
        InspectionPlanDetailVO detail = new InspectionPlanDetailVO();
        detail.setId(7L);
        detail.setFrequencyType(FrequencyType.WEEKLY);
        when(inspectionPlanMapper.getDetailById(7L)).thenReturn(detail);
        when(inspectionPlanMapper.listEquipmentByPlanId(7L))
                .thenReturn(List.of());
        when(inspectionPlanMapper.listItemsByPlanId(7L))
                .thenReturn(List.of());
        when(inspectionPlanMapper.listScheduleDaysByPlanId(7L))
                .thenReturn(List.of(1, 3, 5));

        InspectionPlanDetailVO result = inspectionPlanService.getById(7L);

        assertThat(result.getWeekDays()).containsExactly(1, 3, 5);
        assertThat(result.getMonthDays()).isEmpty();
        assertThat(result.getEquipment()).isEmpty();
        assertThat(result.getInspectionItems()).isEmpty();
    }

    @Test
    void getByIdShouldRejectMissingPlan() {
        when(inspectionPlanMapper.getDetailById(404L)).thenReturn(null);

        assertBusinessError(
                () -> inspectionPlanService.getById(404L),
                ErrorCode.INSPECTION_PLAN_NOT_FOUND
        );
    }

    @Test
    void updateShouldReplaceAllPlanRelations() {
        InspectionPlanSaveDTO dto = validDto();
        InspectionPlan existing = persistedPlan(7L, 1);
        when(inspectionPlanMapper.getByIdForUpdate(7L)).thenReturn(existing);
        stubValidReferences(dto, 1L);
        when(inspectionPlanMapper.getByName(dto.getName().trim()))
                .thenReturn(existing);
        when(inspectionPlanMapper.findConflictCandidates(
                anyList(), any(LocalTime.class), any(LocalDate.class),
                nullable(LocalDate.class), nullable(Long.class)))
                .thenReturn(List.of());

        inspectionPlanService.update(7L, dto);

        ArgumentCaptor<InspectionPlan> captor =
                ArgumentCaptor.forClass(InspectionPlan.class);
        verify(inspectionPlanMapper).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7L);
        assertThat(captor.getValue().getName()).isEqualTo("测试日计划");
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
        verify(inspectionPlanMapper).deleteScheduleRelations(7L);
        verify(inspectionPlanMapper).deleteItemRelations(7L);
        verify(inspectionPlanMapper).deleteEquipmentRelations(7L);
        verify(inspectionPlanMapper).batchInsertEquipment(7L, dto.getEquipmentIds());
        verify(inspectionPlanMapper).batchInsertItems(7L, dto.getInspectionItemIds());
    }

    @Test
    void disableShouldClearNextGenerationTime() {
        when(inspectionPlanMapper.getByIdForUpdate(7L))
                .thenReturn(persistedPlan(7L, 1));
        InspectionPlanStatusDTO dto = new InspectionPlanStatusDTO();
        dto.setEnabled(false);

        inspectionPlanService.changeStatus(7L, dto);

        verify(inspectionPlanMapper).updateStatus(7L, false, null);
        verify(inspectionPlanMapper, never()).listEquipmentIdsByPlanId(7L);
    }

    @Test
    void enableShouldRevalidateStoredConfiguration() {
        InspectionPlan plan = persistedPlan(7L, 0);
        when(inspectionPlanMapper.getByIdForUpdate(7L)).thenReturn(plan);
        when(inspectionPlanMapper.listEquipmentIdsByPlanId(7L))
                .thenReturn(List.of(1L));
        when(inspectionPlanMapper.listItemIdsByPlanId(7L))
                .thenReturn(List.of(10L));
        when(inspectionPlanMapper.listScheduleDaysByPlanId(7L))
                .thenReturn(List.of());
        when(equipmentMapper.getByIds(List.of(1L)))
                .thenReturn(List.of(equipment(1L, 1L)));
        when(userMapper.getById(2L)).thenReturn(inspector());
        when(inspectionItemMapper.getByIds(List.of(10L)))
                .thenReturn(List.of(item(10L, 1L)));
        when(inspectionPlanMapper.findConflictCandidates(
                anyList(), any(LocalTime.class), any(LocalDate.class),
                nullable(LocalDate.class), nullable(Long.class)))
                .thenReturn(List.of());
        InspectionPlanStatusDTO dto = new InspectionPlanStatusDTO();
        dto.setEnabled(true);

        inspectionPlanService.changeStatus(7L, dto);

        verify(inspectionPlanMapper).updateStatus(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(true),
                any(java.time.LocalDateTime.class));
    }

    @Test
    void deleteShouldRejectPlanThatAlreadyGeneratedTasks() {
        when(inspectionPlanMapper.getByIdForUpdate(7L))
                .thenReturn(persistedPlan(7L, 0));
        when(inspectionPlanMapper.countTasks(7L)).thenReturn(1);

        assertBusinessError(
                () -> inspectionPlanService.delete(7L),
                ErrorCode.INSPECTION_PLAN_HAS_TASKS
        );
        verify(inspectionPlanMapper, never()).deleteById(7L);
    }

    @Test
    void deleteShouldRemovePlanWithoutTasks() {
        when(inspectionPlanMapper.getByIdForUpdate(7L))
                .thenReturn(persistedPlan(7L, 0));
        when(inspectionPlanMapper.countTasks(7L)).thenReturn(0);

        inspectionPlanService.delete(7L);

        verify(inspectionPlanMapper).deleteById(7L);
    }

    private InspectionPlanSaveDTO validDto() {
        InspectionPlanSaveDTO dto = new InspectionPlanSaveDTO();
        dto.setName(" 测试日计划 ");
        dto.setInspectorId(2L);
        dto.setFrequencyType(FrequencyType.DAILY);
        dto.setExecutionTime(LocalTime.of(9, 30));
        dto.setStartDate(LocalDate.now().plusDays(10));
        dto.setEndDate(LocalDate.now().plusDays(30));
        dto.setEquipmentIds(List.of(1L));
        dto.setInspectionItemIds(List.of(10L));
        dto.setRemark("   ");
        return dto;
    }

    private void stubValidReferences(InspectionPlanSaveDTO dto, Long itemTypeId) {
        stubValidEquipmentAndInspector(dto);
        when(inspectionItemMapper.getByIds(dto.getInspectionItemIds()))
                .thenReturn(List.of(item(10L, itemTypeId)));
    }

    private void stubValidEquipmentAndInspector(InspectionPlanSaveDTO dto) {
        when(equipmentMapper.getByIds(dto.getEquipmentIds()))
                .thenReturn(List.of(equipment(1L, 1L)));
        when(userMapper.getById(dto.getInspectorId())).thenReturn(inspector());
    }

    private void stubNoExistingPlan(InspectionPlanSaveDTO dto) {
        when(inspectionPlanMapper.getByName(dto.getName().trim())).thenReturn(null);
        when(inspectionPlanMapper.findConflictCandidates(
                anyList(), any(LocalTime.class), any(LocalDate.class),
                nullable(LocalDate.class), nullable(Long.class)))
                .thenReturn(List.of());
    }

    private Equipment equipment(Long id, Long typeId) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setEquipmentTypeId(typeId);
        equipment.setStatus("NORMAL");
        return equipment;
    }

    private InspectionItem item(Long id, Long typeId) {
        InspectionItem item = new InspectionItem();
        item.setId(id);
        item.setEquipmentTypeId(typeId);
        item.setEnabled(1);
        return item;
    }

    private User inspector() {
        User user = new User();
        user.setId(2L);
        user.setEnabled(1);
        user.setRoleCode(RoleCode.INSPECTOR.name());
        return user;
    }

    private InspectionPlan persistedPlan(Long id, Integer enabled) {
        InspectionPlan plan = new InspectionPlan();
        plan.setId(id);
        plan.setName("已保存计划");
        plan.setInspectorId(2L);
        plan.setFrequencyType(FrequencyType.DAILY.name());
        plan.setExecutionTime(LocalTime.of(9, 30));
        plan.setStartDate(LocalDate.now().plusDays(10));
        plan.setEndDate(LocalDate.now().plusDays(30));
        plan.setDeadlineHours(24);
        plan.setEnabled(enabled);
        return plan;
    }

    private void assertBusinessError(Runnable action, ErrorCode errorCode) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(action::run)
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(errorCode));
    }
}
