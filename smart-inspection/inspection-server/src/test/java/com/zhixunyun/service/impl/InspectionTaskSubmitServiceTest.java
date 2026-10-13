package com.zhixunyun.service.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.*;
import com.zhixunyun.entity.FaultRecord;
import com.zhixunyun.entity.InspectionRecord;
import com.zhixunyun.entity.WorkOrder;
import com.zhixunyun.enumeration.*;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.*;
import com.zhixunyun.vo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InspectionTaskSubmitServiceTest {
    private final InspectionTaskMapper taskMapper = mock(InspectionTaskMapper.class);
    private final InspectionRecordMapper recordMapper = mock(InspectionRecordMapper.class);
    private final FaultRecordMapper faultMapper = mock(FaultRecordMapper.class);
    private final WorkOrderMapper workOrderMapper = mock(WorkOrderMapper.class);
    private final EquipmentMapper equipmentMapper = mock(EquipmentMapper.class);
    private final AttachmentMapper attachmentMapper = mock(AttachmentMapper.class);
    private final InspectionTaskServiceImpl service = new InspectionTaskServiceImpl(
            taskMapper, recordMapper, faultMapper, workOrderMapper, equipmentMapper, attachmentMapper);

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(9L);
        BaseContext.setCurrentRole(RoleCode.INSPECTOR.name());
        when(taskMapper.getSubmitInfoById(1L)).thenReturn(task());
        when(taskMapper.getEquipmentSubmitInfoByTaskId(1L))
                .thenReturn(List.of(taskEquipment(11L, 101L), taskEquipment(12L, 102L)));
        when(taskMapper.getTaskItems(1L)).thenReturn(List.of(taskItem()));
    }

    @AfterEach
    void tearDown() {
        BaseContext.remove();
    }

    @Test
    void submitShouldCreateRecordsFaultWorkOrderAndCompleteTask() {
        when(taskMapper.completeTask(eq(1L), eq(2), any())).thenReturn(1);
        when(taskMapper.completeTaskEquipment(eq(1L), any())).thenReturn(2);
        AtomicLong recordId = new AtomicLong(100L);
        doAnswer(invocation -> {
            invocation.<InspectionRecord>getArgument(0).setId(recordId.incrementAndGet());
            return 1;
        }).when(recordMapper).insert(any());
        doAnswer(invocation -> {
            invocation.<FaultRecord>getArgument(0).setId(201L);
            return 1;
        }).when(faultMapper).insert(any());
        doAnswer(invocation -> {
            invocation.<WorkOrder>getArgument(0).setId(301L);
            return 1;
        }).when(workOrderMapper).insert(any());

        InspectionSubmitDataVO result = service.submit(
                1L, request(normalResult(101L), abnormalResult(102L)));

        assertThat(result.getInspectionTaskId()).isEqualTo(1L);
        assertThat(result.getInspectionRecordIds()).containsExactly(101L, 102L);
        assertThat(result.getFaultRecordIds()).containsExactly(201L);
        assertThat(result.getWorkOrderIds()).containsExactly(301L);
        verify(recordMapper, times(2)).insert(any());
        verify(recordMapper, times(2)).insertItem(any());
        verify(faultMapper).insert(any());
        verify(workOrderMapper).insert(any());
        verify(workOrderMapper).insertOperation(any());
        verify(equipmentMapper).updateAfterInspection(eq(101L), eq(false), any());
        verify(equipmentMapper).updateAfterInspection(eq(102L), eq(true), any());
    }

    @Test
    void submitShouldRejectMissingTaskEquipment() {
        assertError(ErrorCode.INSPECTION_TASK_EQUIPMENT_INVALID,
                () -> service.submit(1L, request(normalResult(101L))));
        verify(taskMapper, never()).completeTask(anyLong(), anyInt(), any());
    }

    @Test
    void submitShouldRejectOverallResultContradiction() {
        EquipmentInspectionResultDTO result = normalResult(101L);
        result.getItemResults().get(0).setNormal(false);
        assertError(ErrorCode.INSPECTION_TASK_RESULT_INCONSISTENT,
                () -> service.submit(1L, request(result, abnormalResult(102L))));
    }

    @Test
    void submitShouldRejectInvalidNumberValue() {
        EquipmentInspectionResultDTO result = normalResult(101L);
        result.getItemResults().get(0).setResultValue("不是数字");
        assertError(ErrorCode.INSPECTION_TASK_ITEM_INVALID,
                () -> service.submit(1L, request(result, abnormalResult(102L))));
    }

    @Test
    void submitShouldRejectConcurrentRepeatedSubmission() {
        when(taskMapper.completeTask(eq(1L), eq(2), any())).thenReturn(0);
        assertError(ErrorCode.INSPECTION_TASK_STATUS_CONFLICT,
                () -> service.submit(1L, request(normalResult(101L), abnormalResult(102L))));
        verify(recordMapper, never()).insert(any());
    }

    private InspectionTaskSubmitInfo task() {
        InspectionTaskSubmitInfo task = new InspectionTaskSubmitInfo();
        task.setId(1L);
        task.setInspectorId(9L);
        task.setInspectorName("测试巡检员");
        task.setStatus(InspectionTaskStatus.IN_PROGRESS);
        return task;
    }

    private InspectionTaskEquipmentSubmitInfo taskEquipment(Long id, Long equipmentId) {
        InspectionTaskEquipmentSubmitInfo info = new InspectionTaskEquipmentSubmitInfo();
        info.setTaskEquipmentId(id);
        info.setEquipmentId(equipmentId);
        return info;
    }

    private TaskInspectionItemVO taskItem() {
        TaskInspectionItemVO item = new TaskInspectionItemVO();
        item.setInspectionItemId(21L);
        item.setItemCode("VOLTAGE");
        item.setItemName("输出电压");
        item.setStandardDescription("应处于正常范围");
        item.setResultType(InspectionResultType.NUMBER);
        item.setUnit("V");
        item.setRequired(true);
        return item;
    }

    private EquipmentInspectionResultDTO normalResult(Long equipmentId) {
        EquipmentInspectionResultDTO result = new EquipmentInspectionResultDTO();
        result.setEquipmentId(equipmentId);
        result.setNormal(true);
        result.setItemResults(List.of(itemResult(true, "220")));
        return result;
    }

    private EquipmentInspectionResultDTO abnormalResult(Long equipmentId) {
        EquipmentInspectionResultDTO result = new EquipmentInspectionResultDTO();
        result.setEquipmentId(equipmentId);
        result.setNormal(false);
        result.setItemResults(List.of(itemResult(false, "190")));
        FaultReportDTO fault = new FaultReportDTO();
        fault.setFaultType("电气故障");
        fault.setFaultLevel(FaultLevel.HIGH);
        fault.setDescription("输出电压过低");
        result.setFault(fault);
        return result;
    }

    private InspectionItemResultDTO itemResult(boolean normal, String value) {
        InspectionItemResultDTO item = new InspectionItemResultDTO();
        item.setInspectionItemId(21L);
        item.setResultValue(value);
        item.setNormal(normal);
        return item;
    }

    private InspectionTaskSubmitRequest request(EquipmentInspectionResultDTO... results) {
        InspectionTaskSubmitRequest request = new InspectionTaskSubmitRequest();
        request.setEquipmentResults(List.of(results));
        return request;
    }

    private void assertError(ErrorCode code, Runnable operation) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(operation::run)
                .satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
