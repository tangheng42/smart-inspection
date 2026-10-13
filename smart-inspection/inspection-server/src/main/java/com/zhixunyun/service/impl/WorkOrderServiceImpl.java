package com.zhixunyun.service.impl;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.WorkOrderAssignRequest;
import com.zhixunyun.dto.WorkOrderQueryDTO;
import com.zhixunyun.dto.WorkOrderResultSubmitRequest;
import com.zhixunyun.dto.WorkOrderVerifyRequest;
import com.zhixunyun.annotation.OperationLog;
import com.zhixunyun.entity.BizAttachment;
import com.zhixunyun.entity.Notification;
import com.zhixunyun.entity.User;
import com.zhixunyun.entity.WorkOrder;
import com.zhixunyun.entity.WorkOrderOperation;
import com.zhixunyun.event.NotificationEvent;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.NotificationType;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.enumeration.WorkOrderOperationType;
import com.zhixunyun.enumeration.WorkOrderStatus;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.AttachmentMapper;
import com.zhixunyun.mapper.EquipmentMapper;
import com.zhixunyun.mapper.FaultRecordMapper;
import com.zhixunyun.mapper.NotificationMapper;
import com.zhixunyun.mapper.UserMapper;
import com.zhixunyun.mapper.WorkOrderMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.WorkOrderService;
import com.zhixunyun.service.NotificationEventPublisher;
import com.zhixunyun.vo.FileReferenceVO;
import com.zhixunyun.vo.WorkOrderDetailVO;
import com.zhixunyun.vo.WorkOrderOperationVO;
import com.zhixunyun.vo.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 维修工单状态流转入口。派单、接单、提交结果、验收和返工均通过
 * 状态及版本条件更新控制并发，并在事务内保存操作轨迹及相关业务变化。
 */
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final WorkOrderMapper mapper;
    private final UserMapper userMapper;
    private final EquipmentMapper equipmentMapper;
    private final FaultRecordMapper faultRecordMapper;
    private final AttachmentMapper attachmentMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationEventPublisher notificationEventPublisher;

    /** 维修员只能查询指派给自己的工单，负责人条件由登录身份覆盖。 */
    @Override
    public PageResult<WorkOrderVO> pageQuery(WorkOrderQueryDTO dto) {
        validateRange(dto);
        dto.setWorkOrderNo(trimToNull(dto.getWorkOrderNo()));
        if (RoleCode.MAINTAINER.name().equals(BaseContext.getCurrentRole())) {
            dto.setMaintainerId(BaseContext.getCurrentId());
        }
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<WorkOrderVO> records = mapper.pageQuery(dto);
        return new PageResult<>(dto.getPageNum(), dto.getPageSize(),
                new PageInfo<>(records).getTotal(), records);
    }

    @Override
    public WorkOrderDetailVO getById(Long workOrderId) {
        WorkOrderDetailVO detail = mapper.getDetailById(workOrderId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        checkMaintainerScope(detail.getMaintainerId());
        detail.setFaultImageUrls(mapper.listFaultImageUrls(workOrderId));
        detail.setResultImageUrls(mapper.listResultImageUrls(workOrderId));
        return detail;
    }

    @Override
    public List<WorkOrderOperationVO> listOperations(Long workOrderId) {
        if (!mapper.existsById(workOrderId)) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        checkMaintainerScope(mapper.getMaintainerId(workOrderId));
        return mapper.listOperations(workOrderId);
    }

    /**
     * 管理员可派发待分配工单，或重新派发尚未接单的工单。
     * 工单状态、操作轨迹和站内通知同事务保存；推送在提交后触发。
     */
    @Override
    @Transactional
    @OperationLog(value = "派发维修工单", businessType = "WORK_ORDER")
    public void assign(Long workOrderId, WorkOrderAssignRequest request) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        WorkOrderStatus currentStatus = workOrder.getStatus();
        if (currentStatus != WorkOrderStatus.PENDING_ASSIGNMENT
                && currentStatus != WorkOrderStatus.PENDING_ACCEPTANCE) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }

        User maintainer = userMapper.getById(request.getMaintainerId());
        if (maintainer == null || maintainer.getEnabled() == null
                || maintainer.getEnabled() != 1
                || !RoleCode.MAINTAINER.name().equals(maintainer.getRoleCode())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_MAINTAINER_UNAVAILABLE);
        }

        OffsetDateTime dueAt = request.getDueAt();
        if (dueAt == null || !dueAt.isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_DUE_AT_INVALID);
        }
        LocalDateTime dueAtLocal = dueAt.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        // 旧状态和 version 同时匹配才可派单，避免管理员并发改派覆盖彼此结果。
        if (mapper.assign(workOrderId, maintainer.getId(), dueAtLocal, now,
                currentStatus, workOrder.getVersion()) == 0) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }

        saveOperation(workOrder, currentStatus, WorkOrderStatus.PENDING_ACCEPTANCE,
                currentStatus == WorkOrderStatus.PENDING_ASSIGNMENT
                        ? WorkOrderOperationType.ASSIGN : WorkOrderOperationType.REASSIGN,
                trimToNull(request.getRemark()), now);
        saveNotification(maintainer.getId(), NotificationType.WORK_ORDER_ASSIGNED,
                "您有一张新的维修工单",
                "工单" + workOrder.getWorkOrderNo() + "已派发给您，请及时处理",
                workOrderId);
    }

    /**
     * 被指派的维修员接单后，工单转为处理中，同时更新故障和设备状态。
     * 三处数据库变化和操作记录共用事务。
     */
    @Override
    @Transactional
    @OperationLog(value = "接收维修工单", businessType = "WORK_ORDER")
    public void accept(Long workOrderId) {
        WorkOrder workOrder = requireOwnedWorkOrder(workOrderId);
        requireStatus(workOrder, WorkOrderStatus.PENDING_ACCEPTANCE);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (mapper.accept(workOrderId, BaseContext.getCurrentId(), now,
                workOrder.getVersion()) == 0) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }
        // 接单后工单、故障和设备状态必须一同提交，不能出现已接单但设备仍为普通故障。
        faultRecordMapper.markProcessing(workOrder.getFaultRecordId());
        equipmentMapper.markInRepair(workOrder.getEquipmentId());
        saveOperation(workOrder, WorkOrderStatus.PENDING_ACCEPTANCE,
                WorkOrderStatus.PROCESSING, WorkOrderOperationType.ACCEPT, null, now);
    }

    /**
     * 被指派的维修员仅可提交处理中的工单；附件必须引用本人已上传文件。
     * 状态更新、维修结果、附件关系及操作记录一同提交。
     */
    @Override
    @Transactional
    @OperationLog(value = "提交维修结果", businessType = "WORK_ORDER")
    public void submitResult(Long workOrderId, WorkOrderResultSubmitRequest request) {
        WorkOrder workOrder = requireOwnedWorkOrder(workOrderId);
        requireStatus(workOrder, WorkOrderStatus.PROCESSING);
        String solution = requireText(request.getSolution());
        String resultDescription = requireText(request.getResultDescription());
        Map<String, Long> fileIds = resolveFileIds(request.getImageUrls());
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);

        if (mapper.submitResult(workOrderId, BaseContext.getCurrentId(), solution,
                resultDescription, now, workOrder.getVersion()) == 0) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }
        saveAttachments(workOrderId, request.getImageUrls(), fileIds);
        saveOperation(workOrder, WorkOrderStatus.PROCESSING,
                WorkOrderStatus.PENDING_VERIFICATION, WorkOrderOperationType.SUBMIT_RESULT,
                resultDescription, now);
    }

    /**
     * 验收待验收工单。通过时关闭故障，并仅在设备无其他活动工单时恢复正常；
     * 未通过则保留故障，工单进入拒绝状态供维修员返工。
     */
    @Override
    @Transactional
    @OperationLog(value = "验收维修工单", businessType = "WORK_ORDER")
    public void verify(Long workOrderId, WorkOrderVerifyRequest request) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        requireStatus(workOrder, WorkOrderStatus.PENDING_VERIFICATION);
        String remark = requireText(request.getRemark());
        boolean passed = Boolean.TRUE.equals(request.getPassed());
        WorkOrderStatus toStatus = passed
                ? WorkOrderStatus.COMPLETED : WorkOrderStatus.REJECTED;
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);

        if (mapper.verify(workOrderId, toStatus, BaseContext.getCurrentId(), passed,
                remark, now, workOrder.getVersion()) == 0) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }
        // 只有验收通过才关闭故障；设备恢复正常还需确认不存在其他活动工单。
        if (passed) {
            faultRecordMapper.close(workOrder.getFaultRecordId(), now);
            equipmentMapper.restoreNormalIfNoActiveWorkOrder(workOrder.getEquipmentId());
        }
        saveOperation(workOrder, WorkOrderStatus.PENDING_VERIFICATION, toStatus,
                passed ? WorkOrderOperationType.VERIFY_PASS
                        : WorkOrderOperationType.VERIFY_REJECT,
                remark, now);
        saveNotification(workOrder.getMaintainerId(),
                passed ? NotificationType.VERIFICATION_PASSED
                        : NotificationType.VERIFICATION_REJECTED,
                passed ? "维修工单验收通过" : "维修工单验收未通过",
                "工单" + workOrder.getWorkOrderNo()
                        + (passed ? "已验收通过" : "验收未通过，请根据意见返工"),
                workOrderId);
    }

    /** 验收拒绝后仅原维修员可将工单重新置为处理中。 */
    @Override
    @Transactional
    @OperationLog(value = "返工维修工单", businessType = "WORK_ORDER")
    public void rework(Long workOrderId, String remark) {
        WorkOrder workOrder = requireOwnedWorkOrder(workOrderId);
        requireStatus(workOrder, WorkOrderStatus.REJECTED);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (mapper.rework(workOrderId, BaseContext.getCurrentId(),
                workOrder.getVersion()) == 0) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }
        saveOperation(workOrder, WorkOrderStatus.REJECTED, WorkOrderStatus.PROCESSING,
                WorkOrderOperationType.REWORK, trimToNull(remark), now);
    }

    /** 只允许取消未接单的工单，避免已开展维修时丢失处理过程。 */
    @Override
    @Transactional
    @OperationLog(value = "取消维修工单", businessType = "WORK_ORDER")
    public void cancel(Long workOrderId, String reason) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        if (workOrder.getStatus() != WorkOrderStatus.PENDING_ASSIGNMENT
                && workOrder.getStatus() != WorkOrderStatus.PENDING_ACCEPTANCE) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }
        String normalizedReason = requireText(reason);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (mapper.cancel(workOrderId, normalizedReason, now,
                workOrder.getVersion()) == 0) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }
        saveOperation(workOrder, workOrder.getStatus(), WorkOrderStatus.CANCELLED,
                WorkOrderOperationType.CANCEL, normalizedReason, now);
    }

    private WorkOrder requireWorkOrder(Long workOrderId) {
        WorkOrder workOrder = mapper.getById(workOrderId);
        if (workOrder == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        return workOrder;
    }

    /** 角色权限允许调用接口后，仍需核对工单实际指派给当前维修员。 */
    private WorkOrder requireOwnedWorkOrder(Long workOrderId) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        if (!BaseContext.getCurrentId().equals(workOrder.getMaintainerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return workOrder;
    }

    private void requireStatus(WorkOrder workOrder, WorkOrderStatus expected) {
        if (workOrder.getStatus() != expected) {
            throw new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT);
        }
    }

    private void checkMaintainerScope(Long maintainerId) {
        if (RoleCode.MAINTAINER.name().equals(BaseContext.getCurrentRole())
                && !BaseContext.getCurrentId().equals(maintainerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateRange(WorkOrderQueryDTO dto) {
        if (dto.getCreatedFrom() != null && dto.getCreatedTo() != null
                && dto.getCreatedFrom().isAfter(dto.getCreatedTo())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
    }

    private void saveOperation(WorkOrder workOrder, WorkOrderStatus fromStatus,
                               WorkOrderStatus toStatus,
                               WorkOrderOperationType operationType,
                               String remark, LocalDateTime operatedAt) {
        User operator = userMapper.getById(BaseContext.getCurrentId());
        if (operator == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        WorkOrderOperation operation = new WorkOrderOperation();
        operation.setWorkOrderId(workOrder.getId());
        operation.setOperationType(operationType);
        operation.setFromStatus(fromStatus);
        operation.setToStatus(toStatus);
        operation.setOperatorId(operator.getId());
        operation.setOperatorName(operator.getDisplayName());
        operation.setRemark(remark);
        operation.setOperatedAt(operatedAt);
        mapper.insertOperation(operation);
    }

    /**
     * 通知先写入数据库供离线用户查询，实时推送由发布器安排在事务提交后执行，
     * 避免客户端收到随后被回滚的工单变更。
     */
    private void saveNotification(Long recipientId, NotificationType type,
                                  String title, String content, Long workOrderId) {
        if (recipientId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedBizType("WORK_ORDER");
        notification.setRelatedBizId(workOrderId);
        notificationMapper.insertOrRefresh(notification);
        notificationEventPublisher.publishAfterCommit(new NotificationEvent(
                UUID.randomUUID().toString(), recipientId, type.name(), title,
                content, "WORK_ORDER", workOrderId,
                LocalDateTime.now(BUSINESS_ZONE)));
    }

    private Map<String, Long> resolveFileIds(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new BusinessException(ErrorCode.WORK_ORDER_ATTACHMENT_INVALID);
        }
        Set<String> unique = new HashSet<>();
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.trim().isEmpty()
                    || !unique.add(imageUrl.trim())) {
                throw new BusinessException(ErrorCode.WORK_ORDER_ATTACHMENT_INVALID);
            }
        }
        Map<String, Long> fileIds = new HashMap<>();
        List<FileReferenceVO> files = attachmentMapper.findFilesByUrls(
                new ArrayList<>(unique), BaseContext.getCurrentId());
        files.forEach(file -> fileIds.put(file.getUrl(), file.getId()));
        if (fileIds.size() != unique.size()) {
            throw new BusinessException(ErrorCode.WORK_ORDER_ATTACHMENT_INVALID);
        }
        return fileIds;
    }

    private void saveAttachments(Long workOrderId, List<String> imageUrls,
                                 Map<String, Long> fileIds) {
        for (int index = 0; index < imageUrls.size(); index++) {
            BizAttachment attachment = new BizAttachment();
            attachment.setFileId(fileIds.get(imageUrls.get(index).trim()));
            attachment.setBizType("REPAIR");
            attachment.setBizId(workOrderId);
            attachment.setSortNo(index);
            attachmentMapper.insert(attachment);
        }
    }

    private String requireText(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
