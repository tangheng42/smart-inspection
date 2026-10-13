package com.zhixunyun.service.impl;

import com.zhixunyun.annotation.OperationLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.EquipmentInspectionResultDTO;
import com.zhixunyun.dto.FaultReportDTO;
import com.zhixunyun.dto.InspectionItemResultDTO;
import com.zhixunyun.dto.InspectionTaskPageQueryDTO;
import com.zhixunyun.dto.InspectionTaskSubmitRequest;
import com.zhixunyun.dto.InspectionTaskTodayQueryDTO;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.FaultStatus;
import com.zhixunyun.enumeration.InspectionTaskStatus;
import com.zhixunyun.enumeration.InspectionResultType;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.enumeration.WorkOrderOperationType;
import com.zhixunyun.enumeration.WorkOrderPriority;
import com.zhixunyun.enumeration.WorkOrderStatus;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.entity.BizAttachment;
import com.zhixunyun.entity.FaultRecord;
import com.zhixunyun.entity.InspectionRecord;
import com.zhixunyun.entity.InspectionRecordItem;
import com.zhixunyun.entity.WorkOrder;
import com.zhixunyun.entity.WorkOrderOperation;
import com.zhixunyun.mapper.AttachmentMapper;
import com.zhixunyun.mapper.EquipmentMapper;
import com.zhixunyun.mapper.FaultRecordMapper;
import com.zhixunyun.mapper.InspectionRecordMapper;
import com.zhixunyun.mapper.InspectionTaskMapper;
import com.zhixunyun.mapper.WorkOrderMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.InspectionTaskService;
import com.zhixunyun.vo.InspectionTaskDetailVO;
import com.zhixunyun.vo.InspectionTaskPageVO;
import com.zhixunyun.vo.InspectionTaskStartInfo;
import com.zhixunyun.vo.InspectionTaskSubmitInfo;
import com.zhixunyun.vo.InspectionTaskEquipmentSubmitInfo;
import com.zhixunyun.vo.InspectionSubmitDataVO;
import com.zhixunyun.vo.FileReferenceVO;
import com.zhixunyun.vo.TaskEquipmentVO;
import com.zhixunyun.vo.TaskInspectionItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



/**
 * 巡检任务的查询与执行入口。任务从计划生成时保留设备、项目快照，
 * 提交时校验负责人和快照范围，并在同一事务中写入记录、故障和工单。
 */
@Service
@RequiredArgsConstructor
public class InspectionTaskServiceImpl implements InspectionTaskService {


    private final InspectionTaskMapper inspectionTaskMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final FaultRecordMapper faultRecordMapper;
    private final WorkOrderMapper workOrderMapper;
    private final EquipmentMapper equipmentMapper;
    private final AttachmentMapper attachmentMapper;



    /**
     * 巡检员的查询条件以登录身份覆盖请求参数，避免通过指定他人 ID 查看任务。
     */
    @Override
    public PageResult<InspectionTaskPageVO> pageQuery(
            InspectionTaskPageQueryDTO dto) {


        // 校验时间范围
        if (dto.getTaskDateFrom() != null
                && dto.getTaskDateTo() != null
                && dto.getTaskDateFrom()
                .isAfter(dto.getTaskDateTo())) {

            throw new BusinessException(
                    ErrorCode.PARAM_VALIDATION_FAILED
            );
        }


        String currentRole =
                BaseContext.getCurrentRole();


        Long currentUserId =
                BaseContext.getCurrentId();



        // 接口角色校验只决定能否调用；这里进一步限制可见的任务数据。
        if (RoleCode.INSPECTOR.name()
                .equals(currentRole)) {

            dto.setInspectorId(currentUserId);
        }



        PageHelper.startPage(
                dto.getPageNum(),
                dto.getPageSize()
        );


        Page<InspectionTaskPageVO> page =
                (Page<InspectionTaskPageVO>)
                        inspectionTaskMapper.pageQuery(dto);



        return new PageResult<>(
                dto.getPageNum(),
                dto.getPageSize(),
                page.getTotal(),
                page.getResult()
        );
    }




    /**
     * 查询今日巡检任务
     */
    @Override
    public PageResult<InspectionTaskPageVO> pageToday(
            InspectionTaskTodayQueryDTO dto) {


        Long currentUserId =
                BaseContext.getCurrentId();


        PageHelper.startPage(
                dto.getPageNum(),
                dto.getPageSize()
        );


        Page<InspectionTaskPageVO> page =
                (Page<InspectionTaskPageVO>)
                        inspectionTaskMapper.pageToday(
                                currentUserId,
                                LocalDate.now(),
                                dto.getStatus()
                        );


        return new PageResult<>(
                dto.getPageNum(),
                dto.getPageSize(),
                page.getTotal(),
                page.getResult()
        );
    }




    /**
     * 详情按任务快照组装，巡检员还需通过任务归属校验。
     * 计划或设备档案后续修改不应改变已生成任务的展示内容。
     */
    @Override
    public InspectionTaskDetailVO getById(
            Long inspectionTaskId) {


        InspectionTaskDetailVO detail =
                inspectionTaskMapper.getDetailById(
                        inspectionTaskId
                );


        if (detail == null) {

            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_NOT_FOUND
            );
        }



        // 详情按 ID 查询，不能依赖列表接口中的负责人过滤。
        String role =
                BaseContext.getCurrentRole();


        if (RoleCode.INSPECTOR.name()
                .equals(role)) {


            Long currentUserId =
                    BaseContext.getCurrentId();


            if (!currentUserId.equals(
                    detail.getInspectorId())) {


                throw new BusinessException(
                        ErrorCode.FORBIDDEN
                );
            }
        }



        List<TaskEquipmentVO> equipmentList =
                inspectionTaskMapper
                        .getTaskEquipment(
                                inspectionTaskId
                        );


        List<TaskInspectionItemVO> itemList =
                inspectionTaskMapper
                        .getTaskItems(
                                inspectionTaskId
                        );



        for(TaskEquipmentVO equipment:
                equipmentList){

            equipment.setInspectionItems(
                    itemList
            );
        }


        detail.setEquipment(
                equipmentList
        );


        return detail;
    }




    /**
     * 仅任务负责人可开始待执行任务。SQL 再以 PENDING 为条件更新，
     * 防止读取状态后其他请求已抢先开始或取消任务。
     */
    @Override
    @Transactional
    @OperationLog(value = "开始巡检任务", businessType = "INSPECTION_TASK")
    public void start(Long inspectionTaskId) {


        Long currentUserId =
                BaseContext.getCurrentId();



        InspectionTaskStartInfo task =
                inspectionTaskMapper
                        .getStartInfoById(
                                inspectionTaskId
                        );


        if(task == null){

            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_NOT_FOUND
            );
        }



        // 只能开始自己的任务
        if(!currentUserId.equals(
                task.getInspectorId())){


            throw new BusinessException(
                    ErrorCode.FORBIDDEN
            );
        }




        // 只有待执行任务才能开始
        if(task.getStatus()
                != InspectionTaskStatus.PENDING){


            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_STATUS_CONFLICT
            );
        }




        int rows =
                inspectionTaskMapper.startTask(
                        inspectionTaskId,
                        LocalDateTime.now()
                );



        // 防止并发重复开始
        if(rows == 0){

            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_STATUS_CONFLICT
            );
        }

    }




    /**
     * 管理员仅能取消待执行任务；条件更新阻止与开始操作并发时互相覆盖。
     */
    @Override
    @OperationLog(value = "取消巡检任务", businessType = "INSPECTION_TASK")
    public void cancel(
            Long inspectionTaskId,
            String reason) {


        InspectionTaskStartInfo task =
                inspectionTaskMapper
                        .getStartInfoById(
                                inspectionTaskId
                        );


        if(task == null){

            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_NOT_FOUND
            );
        }



        if(task.getStatus()
                != InspectionTaskStatus.PENDING){


            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_STATUS_CONFLICT
            );
        }



        int rows =
                inspectionTaskMapper.cancelTask(
                        inspectionTaskId,
                        reason.trim()
                );


        if(rows == 0){

            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_STATUS_CONFLICT
            );
        }

    }





    /**
     * 提交执行中任务的全部设备和巡检项目结果。
     * 先核对任务归属、快照范围、结果与附件，再以旧状态条件更新抢占提交权；
     * 记录、异常、维修工单、设备状态和任务完成状态处于同一事务，任一步失败均回滚。
     * 重复提交无法再次完成 IN_PROGRESS 到 COMPLETED 的更新。
     */
    @Override
    @Transactional
    @OperationLog(value = "提交巡检结果", businessType = "INSPECTION_TASK")
    public InspectionSubmitDataVO submit(
            Long inspectionTaskId,
            InspectionTaskSubmitRequest request) {


        // 先校验负责人及任务快照，避免客户端漏报设备、项目或伪造非本任务的结果。
        InspectionTaskSubmitInfo task =
                checkTask(inspectionTaskId);


        List<InspectionTaskEquipmentSubmitInfo> taskEquipment =
                inspectionTaskMapper.getEquipmentSubmitInfoByTaskId(inspectionTaskId);
        List<TaskInspectionItemVO> taskItems = inspectionTaskMapper.getTaskItems(inspectionTaskId);

        validateSubmission(taskEquipment, taskItems, request.getEquipmentResults());
        Map<String, Long> fileIdsByUrl = resolveFileIds(request.getEquipmentResults());

        LocalDateTime completedAt = LocalDateTime.now();
        // 旧状态条件使并发请求只有一个能写入后续记录；失败时提交权随事务回滚。
        if (inspectionTaskMapper.completeTask(
                inspectionTaskId, taskEquipment.size(), completedAt) == 0) {
            throw new BusinessException(ErrorCode.INSPECTION_TASK_STATUS_CONFLICT);
        }

        Map<Long, Long> taskEquipmentIdByEquipmentId = new HashMap<>();
        taskEquipment.forEach(item -> taskEquipmentIdByEquipmentId.put(
                item.getEquipmentId(), item.getTaskEquipmentId()));
        Map<Long, TaskInspectionItemVO> taskItemById = new HashMap<>();
        taskItems.forEach(item -> taskItemById.put(item.getInspectionItemId(), item));

        List<Long> recordIds = new ArrayList<>();
        List<Long> faultIds = new ArrayList<>();
        List<Long> workOrderIds = new ArrayList<>();

        for (EquipmentInspectionResultDTO equipmentResult : request.getEquipmentResults()) {
            InspectionRecord record = buildInspectionRecord(
                    inspectionTaskId, task, equipmentResult,
                    taskEquipmentIdByEquipmentId.get(equipmentResult.getEquipmentId()), completedAt);
            inspectionRecordMapper.insert(record);
            recordIds.add(record.getId());

            for (InspectionItemResultDTO itemResult : equipmentResult.getItemResults()) {
                inspectionRecordMapper.insertItem(buildRecordItem(
                        record.getId(), taskItemById.get(itemResult.getInspectionItemId()), itemResult));
            }
            saveAttachments(equipmentResult.getImageUrls(), "INSPECTION", record.getId(), fileIdsByUrl);

            if (!equipmentResult.getNormal()) {
                FaultRecord fault = createFault(
                        inspectionTaskId, record.getId(), task.getInspectorId(),
                        equipmentResult, completedAt);
                faultIds.add(fault.getId());
                saveAttachments(equipmentResult.getFault().getImageUrls(),
                        "FAULT", fault.getId(), fileIdsByUrl);

                WorkOrder workOrder = createWorkOrder(fault, task, completedAt);
                workOrderIds.add(workOrder.getId());
            }

            equipmentMapper.updateAfterInspection(
                    equipmentResult.getEquipmentId(),
                    !equipmentResult.getNormal(),
                    completedAt);
        }

        int completedEquipment = inspectionTaskMapper.completeTaskEquipment(
                inspectionTaskId, completedAt);
        if (completedEquipment != taskEquipment.size()) {
            throw new BusinessException(ErrorCode.INSPECTION_TASK_STATUS_CONFLICT);
        }

        return new InspectionSubmitDataVO(
                inspectionTaskId, recordIds, faultIds, workOrderIds);
    }

    /**
     * 提交巡检前任务校验
     */
    private InspectionTaskSubmitInfo checkTask(
            Long inspectionTaskId){


        InspectionTaskSubmitInfo task =
                inspectionTaskMapper
                        .getSubmitInfoById(
                                inspectionTaskId
                        );



        if(task == null){

            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_NOT_FOUND
            );
        }



        Long currentUserId =
                BaseContext.getCurrentId();



        // 只能负责人提交
        if(!currentUserId.equals(
                task.getInspectorId())){


            throw new BusinessException(
                    ErrorCode.FORBIDDEN
            );
        }



        // 只能提交执行中的任务
        if(task.getStatus()
                != InspectionTaskStatus.IN_PROGRESS){


            throw new BusinessException(
                    ErrorCode.INSPECTION_TASK_STATUS_CONFLICT
            );
        }



        return task;
    }




    /**
     * 每台设备必须恰好提交任务快照中的全部项目；设备级正常标记须与项目结果一致，
     * 异常设备必须携带故障信息，正常设备则不得携带故障信息。
     */
    private void validateSubmission(
            List<InspectionTaskEquipmentSubmitInfo> taskEquipment,
            List<TaskInspectionItemVO> taskItems,
            List<EquipmentInspectionResultDTO> results) {
        Set<Long> expectedEquipmentIds = new HashSet<>();
        taskEquipment.forEach(item -> expectedEquipmentIds.add(item.getEquipmentId()));
        Set<Long> submittedEquipmentIds = new HashSet<>();
        for (EquipmentInspectionResultDTO result : results) {
            if (!submittedEquipmentIds.add(result.getEquipmentId())) {
                throw new BusinessException(ErrorCode.INSPECTION_TASK_EQUIPMENT_INVALID);
            }
        }
        if (!submittedEquipmentIds.equals(expectedEquipmentIds)) {
            throw new BusinessException(ErrorCode.INSPECTION_TASK_EQUIPMENT_INVALID);
        }

        Set<Long> expectedItemIds = new HashSet<>();
        Map<Long, TaskInspectionItemVO> snapshots = new HashMap<>();
        taskItems.forEach(item -> {
            expectedItemIds.add(item.getInspectionItemId());
            snapshots.put(item.getInspectionItemId(), item);
        });

        for (EquipmentInspectionResultDTO result : results) {
            Set<Long> submittedItemIds = new HashSet<>();
            for (InspectionItemResultDTO item : result.getItemResults()) {
                if (!submittedItemIds.add(item.getInspectionItemId())) {
                    throw new BusinessException(ErrorCode.INSPECTION_TASK_ITEM_INVALID);
                }
                validateResultValue(snapshots.get(item.getInspectionItemId()), item.getResultValue());
            }
            if (!submittedItemIds.equals(expectedItemIds)) {
                throw new BusinessException(ErrorCode.INSPECTION_TASK_ITEM_INVALID);
            }

            boolean allItemsNormal = result.getItemResults().stream()
                    .allMatch(item -> Boolean.TRUE.equals(item.getNormal()));
            if (result.getNormal() != allItemsNormal) {
                throw new BusinessException(ErrorCode.INSPECTION_TASK_RESULT_INCONSISTENT);
            }
            if ((!result.getNormal() && result.getFault() == null)
                    || (result.getNormal() && result.getFault() != null)) {
                throw new BusinessException(ErrorCode.INSPECTION_TASK_FAULT_INVALID);
            }
            validateNoDuplicateUrls(result.getImageUrls());
            if (result.getFault() != null) {
                validateNoDuplicateUrls(result.getFault().getImageUrls());
            }
        }
    }

    /**
     * 校验巡检结果值是否符合类型要求
     */
    private void validateResultValue(TaskInspectionItemVO snapshot, String value) {
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.INSPECTION_TASK_ITEM_INVALID);
        }
        String trimmed = value.trim();
        try {
            if (snapshot.getResultType() == InspectionResultType.NUMBER) {
                new BigDecimal(trimmed);
            } else if (snapshot.getResultType() == InspectionResultType.BOOLEAN
                    && !("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.INSPECTION_TASK_ITEM_INVALID);
        }
    }

    /**
     * 构建巡检记录
     */
    private InspectionRecord buildInspectionRecord(
            Long taskId, InspectionTaskSubmitInfo task,
            EquipmentInspectionResultDTO result, Long taskEquipmentId,
            LocalDateTime completedAt) {
        InspectionRecord record = new InspectionRecord();
        record.setTaskId(taskId);
        record.setTaskEquipmentId(taskEquipmentId);
        record.setEquipmentId(result.getEquipmentId());
        record.setInspectorId(task.getInspectorId());
        record.setNormal(result.getNormal());
        record.setRemark(trimToNull(result.getRemark()));
        record.setCompletedAt(completedAt);
        return record;
    }

    /**
     * 构建巡检记录项
     */
    private InspectionRecordItem buildRecordItem(
            Long recordId, TaskInspectionItemVO snapshot, InspectionItemResultDTO result) {
        InspectionRecordItem item = new InspectionRecordItem();
        item.setInspectionRecordId(recordId);
        item.setInspectionItemId(snapshot.getInspectionItemId());
        item.setItemCodeSnapshot(snapshot.getItemCode());
        item.setItemNameSnapshot(snapshot.getItemName());
        item.setStandardDescriptionSnapshot(snapshot.getStandardDescription());
        item.setResultTypeSnapshot(snapshot.getResultType());
        item.setResultValue(result.getResultValue().trim());
        item.setUnitSnapshot(snapshot.getUnit());
        item.setNormal(result.getNormal());
        item.setRemark(trimToNull(result.getRemark()));
        return item;
    }

    /**
     * 创建巡检异常记录
     */
    private FaultRecord createFault(
            Long taskId, Long recordId, Long reporterId,
            EquipmentInspectionResultDTO result, LocalDateTime now) {
        FaultReportDTO dto = result.getFault();
        FaultRecord fault = new FaultRecord();
        fault.setFaultNo(generateBusinessNo("FR"));
        fault.setInspectionTaskId(taskId);
        fault.setInspectionRecordId(recordId);
        fault.setEquipmentId(result.getEquipmentId());
        fault.setReporterId(reporterId);
        fault.setFaultType(dto.getFaultType().trim());
        fault.setFaultLevel(dto.getFaultLevel());
        fault.setDescription(dto.getDescription().trim());
        fault.setStatus(FaultStatus.ORDER_CREATED);
        fault.setReportedAt(now);
        faultRecordMapper.insert(fault);
        return fault;
    }

    /**
     * 创建巡检异常工单
     */
    private WorkOrder createWorkOrder(
            FaultRecord fault, InspectionTaskSubmitInfo task, LocalDateTime now) {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderNo(generateBusinessNo("WO"));
        order.setFaultRecordId(fault.getId());
        order.setEquipmentId(fault.getEquipmentId());
        order.setPriority(WorkOrderPriority.valueOf(fault.getFaultLevel().name()));
        order.setStatus(WorkOrderStatus.PENDING_ASSIGNMENT);
        workOrderMapper.insert(order);

        WorkOrderOperation operation = new WorkOrderOperation();
        operation.setWorkOrderId(order.getId());
        operation.setOperationType(WorkOrderOperationType.CREATE);
        operation.setToStatus(WorkOrderStatus.PENDING_ASSIGNMENT);
        operation.setOperatorId(task.getInspectorId());
        operation.setOperatorName(task.getInspectorName());
        operation.setRemark("巡检异常自动创建工单");
        operation.setOperatedAt(now);
        workOrderMapper.insertOperation(operation);
        return order;
    }

    /**
     * 解析巡检结果中的文件URL
     */
    private Map<String, Long> resolveFileIds(List<EquipmentInspectionResultDTO> results) {
        Set<String> urls = new LinkedHashSet<>();
        for (EquipmentInspectionResultDTO result : results) {
            addUrls(urls, result.getImageUrls());
            if (result.getFault() != null) addUrls(urls, result.getFault().getImageUrls());
        }
        if (urls.isEmpty()) return Map.of();
        Map<String, Long> fileIds = new HashMap<>();
        List<FileReferenceVO> files = attachmentMapper.findFilesByUrls(
                new ArrayList<>(urls), BaseContext.getCurrentId());
        files.forEach(file -> fileIds.put(file.getUrl(), file.getId()));
        if (fileIds.size() != urls.size()) {
            throw new BusinessException(ErrorCode.INSPECTION_TASK_ATTACHMENT_INVALID);
        }
        return fileIds;
    }

    /**
     * 保存巡检记录附件
     */
    private void saveAttachments(
            List<String> urls, String bizType, Long bizId, Map<String, Long> fileIds) {
        if (urls == null) return;
        for (int i = 0; i < urls.size(); i++) {
            BizAttachment attachment = new BizAttachment();
            attachment.setFileId(fileIds.get(urls.get(i).trim()));
            attachment.setBizType(bizType);
            attachment.setBizId(bizId);
            attachment.setSortNo(i);
            attachmentMapper.insert(attachment);
        }
    }

    /**
     * 校验巡检记录附件URL是否重复
     */
    private void validateNoDuplicateUrls(List<String> urls) {
        if (urls == null) return;
        Set<String> unique = new HashSet<>();
        for (String url : urls) {
            if (url == null || url.trim().isEmpty() || !unique.add(url.trim())) {
                throw new BusinessException(ErrorCode.INSPECTION_TASK_ATTACHMENT_INVALID);
            }
        }
    }

    /**
     * 添加URL到集合中
     */
    private void addUrls(Set<String> target, List<String> urls) {
        if (urls != null) urls.forEach(url -> target.add(url.trim()));
    }

    /**
     * 去空格并判断是否为空
     */
    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 生成业务编号
     */
    private String generateBusinessNo(String prefix) {
        String time = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return prefix + time + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

}
