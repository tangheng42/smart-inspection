package com.zhixunyun.service.impl;

import com.zhixunyun.context.BaseContext;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.enumeration.InspectionTaskStatus;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.FaultRecordMapper;
import com.zhixunyun.mapper.InspectionRecordMapper;
import com.zhixunyun.mapper.InspectionTaskMapper;
import com.zhixunyun.mapper.NotificationMapper;
import com.zhixunyun.mapper.WorkOrderMapper;
import com.zhixunyun.mapper.UserMapper;
import com.zhixunyun.mapper.EquipmentMapper;
import com.zhixunyun.mapper.AttachmentMapper;
import com.zhixunyun.service.NotificationEventPublisher;
import com.zhixunyun.vo.FaultRecordDetailVO;
import com.zhixunyun.vo.InspectionRecordDetailVO;
import com.zhixunyun.vo.InspectionTaskStartInfo;
import com.zhixunyun.vo.WorkOrderDetailVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class AccessScopeServiceTest {

    @AfterEach
    void clearContext() {
        BaseContext.remove();
    }

    @Test
    void inspectorCannotReadAnotherInspectorsRecord() {
        InspectionRecordMapper mapper = mock(InspectionRecordMapper.class);
        InspectionRecordDetailVO detail = new InspectionRecordDetailVO();
        detail.setInspectorId(22L);
        when(mapper.getDetailById(1L)).thenReturn(detail);
        BaseContext.setCurrentId(11L);
        BaseContext.setCurrentRole(RoleCode.INSPECTOR.name());

        assertForbidden(() -> new InspectionRecordServiceImpl(mapper).getById(1L));
        verify(mapper, never()).listItemResults(anyLong());
    }

    @Test
    void maintainerCannotReadUnassignedFault() {
        FaultRecordMapper mapper = mock(FaultRecordMapper.class);
        FaultRecordDetailVO detail = new FaultRecordDetailVO();
        when(mapper.getDetailById(1L)).thenReturn(detail);
        when(mapper.maintainerCanAccess(1L, 11L)).thenReturn(false);
        BaseContext.setCurrentId(11L);
        BaseContext.setCurrentRole(RoleCode.MAINTAINER.name());

        assertForbidden(() -> new FaultRecordServiceImpl(mapper).getById(1L));
        verify(mapper, never()).listImageUrls(anyLong());
    }

    @Test
    void maintainerCannotReadAnotherMaintainersWorkOrder() {
        WorkOrderMapper mapper = mock(WorkOrderMapper.class);
        WorkOrderDetailVO detail = new WorkOrderDetailVO();
        detail.setMaintainerId(22L);
        when(mapper.getDetailById(1L)).thenReturn(detail);
        BaseContext.setCurrentId(11L);
        BaseContext.setCurrentRole(RoleCode.MAINTAINER.name());

        assertForbidden(() -> new WorkOrderServiceImpl(
                mapper,
                mock(UserMapper.class),
                mock(EquipmentMapper.class),
                mock(FaultRecordMapper.class),
                mock(AttachmentMapper.class),
                mock(NotificationMapper.class),
                mock(NotificationEventPublisher.class)).getById(1L));
        verify(mapper, never()).listFaultImageUrls(anyLong());
    }

    @Test
    void userCannotMarkAnotherUsersNotificationRead() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.getRecipientId(1L)).thenReturn(22L);
        BaseContext.setCurrentId(11L);

        assertForbidden(() -> new NotificationServiceImpl(mapper).markRead(1L));
        verify(mapper, never()).markRead(anyLong(), anyLong());
    }

    @Test
    void markingOwnAlreadyReadNotificationIsIdempotent() {
        NotificationMapper mapper = mock(NotificationMapper.class);
        when(mapper.getRecipientId(1L)).thenReturn(11L);
        when(mapper.markRead(1L, 11L)).thenReturn(0);
        BaseContext.setCurrentId(11L);

        new NotificationServiceImpl(mapper).markRead(1L);

        verify(mapper).markRead(1L, 11L);
    }

    @Test
    void cancelTaskRejectsConcurrentStateChange() {
        InspectionTaskMapper mapper = mock(InspectionTaskMapper.class);
        InspectionTaskStartInfo task = new InspectionTaskStartInfo();
        task.setStatus(InspectionTaskStatus.PENDING);
        when(mapper.getStartInfoById(1L)).thenReturn(task);
        when(mapper.cancelTask(1L, "临时停用")).thenReturn(0);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> new InspectionTaskServiceImpl(
                        mapper, null, null, null, null, null)
                        .cancel(1L, " 临时停用 "))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INSPECTION_TASK_STATUS_CONFLICT));
    }

    private void assertForbidden(Runnable operation) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(operation::run)
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }
}
