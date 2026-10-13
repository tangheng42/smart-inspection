package com.zhixunyun.service.impl;

import java.time.OffsetDateTime;
import java.util.List;

import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.WorkOrderAssignRequest;
import com.zhixunyun.dto.WorkOrderResultSubmitRequest;
import com.zhixunyun.dto.WorkOrderVerifyRequest;
import com.zhixunyun.entity.User;
import com.zhixunyun.entity.WorkOrder;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.enumeration.WorkOrderStatus;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.AttachmentMapper;
import com.zhixunyun.mapper.EquipmentMapper;
import com.zhixunyun.mapper.FaultRecordMapper;
import com.zhixunyun.mapper.NotificationMapper;
import com.zhixunyun.mapper.UserMapper;
import com.zhixunyun.mapper.WorkOrderMapper;
import com.zhixunyun.service.NotificationEventPublisher;
import com.zhixunyun.vo.FileReferenceVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkOrderServiceImplTest {

    private final WorkOrderMapper mapper = mock(WorkOrderMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final EquipmentMapper equipmentMapper = mock(EquipmentMapper.class);
    private final FaultRecordMapper faultMapper = mock(FaultRecordMapper.class);
    private final AttachmentMapper attachmentMapper = mock(AttachmentMapper.class);
    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final NotificationEventPublisher eventPublisher =
            mock(NotificationEventPublisher.class);
    private final WorkOrderServiceImpl service = new WorkOrderServiceImpl(
            mapper, userMapper, equipmentMapper, faultMapper,
            attachmentMapper, notificationMapper, eventPublisher);

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(3L);
        BaseContext.setCurrentRole(RoleCode.MAINTAINER.name());
        when(userMapper.getById(3L)).thenReturn(user(3L, RoleCode.MAINTAINER));
    }

    @AfterEach
    void tearDown() {
        BaseContext.remove();
    }

    @Test
    void assignShouldUseVersionAndCreateNotification() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentRole(RoleCode.ADMIN.name());
        when(userMapper.getById(1L)).thenReturn(user(1L, RoleCode.ADMIN));
        when(mapper.getById(10L)).thenReturn(order(WorkOrderStatus.PENDING_ASSIGNMENT, null));
        when(mapper.assign(eq(10L), eq(3L), any(), any(),
                eq(WorkOrderStatus.PENDING_ASSIGNMENT), eq(0))).thenReturn(1);

        WorkOrderAssignRequest request = new WorkOrderAssignRequest();
        request.setMaintainerId(3L);
        request.setDueAt(OffsetDateTime.now().plusDays(1));
        service.assign(10L, request);

        verify(mapper).insertOperation(any());
        verify(notificationMapper).insertOrRefresh(any());
        verify(eventPublisher).publishAfterCommit(any());
    }

    @Test
    void acceptShouldSynchronizeFaultAndEquipment() {
        when(mapper.getById(10L)).thenReturn(order(WorkOrderStatus.PENDING_ACCEPTANCE, 3L));
        when(mapper.accept(eq(10L), eq(3L), any(), eq(0))).thenReturn(1);

        service.accept(10L);

        verify(faultMapper).markProcessing(20L);
        verify(equipmentMapper).markInRepair(30L);
        verify(mapper).insertOperation(any());
    }

    @Test
    void submitResultShouldRequireOwnedUploadedImages() {
        when(mapper.getById(10L)).thenReturn(order(WorkOrderStatus.PROCESSING, 3L));
        when(attachmentMapper.findFilesByUrls(any(), eq(3L)))
                .thenReturn(List.of(file(40L, "http://localhost/image.jpg")));
        when(mapper.submitResult(eq(10L), eq(3L), anyString(), anyString(),
                any(), eq(0))).thenReturn(1);
        WorkOrderResultSubmitRequest request = new WorkOrderResultSubmitRequest();
        request.setSolution("更换模块");
        request.setResultDescription("设备恢复正常");
        request.setImageUrls(List.of("http://localhost/image.jpg"));

        service.submitResult(10L, request);

        verify(attachmentMapper).insert(any());
        verify(mapper).insertOperation(any());
    }

    @Test
    void verifyPassedShouldCloseFaultAndSafelyRestoreEquipment() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentRole(RoleCode.ADMIN.name());
        when(userMapper.getById(1L)).thenReturn(user(1L, RoleCode.ADMIN));
        when(mapper.getById(10L)).thenReturn(order(WorkOrderStatus.PENDING_VERIFICATION, 3L));
        when(mapper.verify(eq(10L), eq(WorkOrderStatus.COMPLETED), eq(1L),
                eq(true), anyString(), any(), eq(0))).thenReturn(1);
        WorkOrderVerifyRequest request = new WorkOrderVerifyRequest();
        request.setPassed(true);
        request.setRemark("现场复测正常");

        service.verify(10L, request);

        verify(faultMapper).close(eq(20L), any());
        verify(equipmentMapper).restoreNormalIfNoActiveWorkOrder(30L);
        verify(notificationMapper).insertOrRefresh(any());
    }

    @Test
    void maintainerCannotOperateAnotherMaintainersOrder() {
        when(mapper.getById(10L)).thenReturn(order(WorkOrderStatus.PENDING_ACCEPTANCE, 4L));

        assertError(ErrorCode.FORBIDDEN, () -> service.accept(10L));

        verify(mapper, never()).accept(anyLong(), anyLong(), any(), anyInt());
    }

    @Test
    void concurrentStateChangeShouldBeRejected() {
        when(mapper.getById(10L)).thenReturn(order(WorkOrderStatus.PENDING_ACCEPTANCE, 3L));
        when(mapper.accept(eq(10L), eq(3L), any(), eq(0))).thenReturn(0);

        assertError(ErrorCode.WORK_ORDER_STATUS_CONFLICT, () -> service.accept(10L));

        verify(faultMapper, never()).markProcessing(anyLong());
    }

    private WorkOrder order(WorkOrderStatus status, Long maintainerId) {
        WorkOrder order = new WorkOrder();
        order.setId(10L);
        order.setWorkOrderNo("WO202609120001");
        order.setFaultRecordId(20L);
        order.setEquipmentId(30L);
        order.setMaintainerId(maintainerId);
        order.setStatus(status);
        order.setVersion(0);
        return order;
    }

    private User user(Long id, RoleCode role) {
        User user = new User();
        user.setId(id);
        user.setDisplayName("测试用户" + id);
        user.setRoleCode(role.name());
        user.setEnabled(1);
        return user;
    }

    private FileReferenceVO file(Long id, String url) {
        FileReferenceVO file = new FileReferenceVO();
        file.setId(id);
        file.setUrl(url);
        return file;
    }

    private void assertError(ErrorCode errorCode, Runnable operation) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(operation::run)
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(errorCode));
    }
}
