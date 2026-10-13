package com.zhixunyun.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhixunyun.dto.InspectionPlanPageQueryDTO;
import com.zhixunyun.dto.InspectionPlanSaveDTO;
import com.zhixunyun.dto.InspectionPlanStatusDTO;
import com.zhixunyun.annotation.OperationLog;
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
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.InspectionPlanService;
import com.zhixunyun.vo.InspectionPlanConflictVO;
import com.zhixunyun.vo.InspectionPlanDetailVO;
import com.zhixunyun.vo.InspectionPlanPageVO;
import com.zhixunyun.vo.InspectionPlanScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理巡检计划的配置、启停和执行时间冲突校验。
 * 计划变更只影响后续任务；已生成任务使用生成时保存的快照。
 */
@Service
@RequiredArgsConstructor
public class InspectionPlanServiceImpl implements InspectionPlanService {

    private final UserMapper userMapper;
    private final EquipmentMapper equipmentMapper;
    private final InspectionItemMapper inspectionItemMapper;
    private final InspectionPlanMapper inspectionPlanMapper;

    /**
     * 创建计划及设备、巡检项目、周期明细。
     * 先锁定关联设备，再检查同设备同执行时刻的计划冲突；主从表写入处于同一事务，
     * 计划名称由业务校验和数据库唯一索引共同约束。
     */
    @Override
    @Transactional
    @OperationLog(value = "创建巡检计划", businessType = "INSPECTION_PLAN")
    public Long create(InspectionPlanSaveDTO dto) {

        normalize(dto);

        // 先排除无效周期和重复关联 ID，避免无意义地持有后续设备行锁。
        validateDuplicateIds(
                dto.getEquipmentIds(),
                dto.getInspectionItemIds()
        );
        validateSchedule(dto);

        LocalDateTime nextGenerateAt = calculateNextGenerateAt(dto);
        if (nextGenerateAt == null) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NO_FUTURE_EXECUTION
            );
        }

        // 先锁定设备。并发创建包含同一设备的计划时，后来的事务会在此等待，
        // 从而能在冲突查询中看到前一个事务刚提交的计划。
        List<Equipment> equipmentList =
                validateEquipment(dto.getEquipmentIds());

        validateInspector(dto.getInspectorId());

        List<InspectionItem> inspectionItems =
                validateInspectionItems(dto.getInspectionItemIds());

        validateEquipmentAndItems(
                equipmentList,
                inspectionItems
        );

        InspectionPlan existing =
                inspectionPlanMapper.getByName(dto.getName());

        if (existing != null) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NAME_EXISTS
            );
        }

        // SQL 缩小候选集后，再按实际执行日期判断周期是否重合。
        checkScheduleConflict(dto, null);

        InspectionPlan plan = buildPlan(
                null, dto, 1, nextGenerateAt);

        try {
            inspectionPlanMapper.insert(plan);
        } catch (DuplicateKeyException ex) {
            // 数据库唯一索引兜住并发创建同名计划的竞态。
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NAME_EXISTS
            );
        }

        Long planId = plan.getId();

        inspectionPlanMapper.batchInsertEquipment(
                planId,
                dto.getEquipmentIds()
        );

        inspectionPlanMapper.batchInsertItems(
                planId,
                dto.getInspectionItemIds()
        );

        List<Integer> dayValues =
                switch (dto.getFrequencyType()) {

                    case DAILY ->
                            List.of();

                    case WEEKLY ->
                            dto.getWeekDays();

                    case MONTHLY ->
                            dto.getMonthDays();
                };

        if (!dayValues.isEmpty()) {
            inspectionPlanMapper.batchInsertSchedule(
                    planId,
                    dayValues
            );
        }

        return planId;
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionPlanDetailVO getById(Long inspectionPlanId) {
        InspectionPlanDetailVO detail =
                inspectionPlanMapper.getDetailById(inspectionPlanId);
        if (detail == null) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NOT_FOUND);
        }

        detail.setEquipment(
                inspectionPlanMapper.listEquipmentByPlanId(inspectionPlanId));
        detail.setInspectionItems(
                inspectionPlanMapper.listItemsByPlanId(inspectionPlanId));

        List<Integer> scheduleDays =
                inspectionPlanMapper.listScheduleDaysByPlanId(inspectionPlanId);
        detail.setWeekDays(
                detail.getFrequencyType() == FrequencyType.WEEKLY
                        ? scheduleDays : List.of());
        detail.setMonthDays(
                detail.getFrequencyType() == FrequencyType.MONTHLY
                        ? scheduleDays : List.of());
        return detail;
    }

    /**
     * 修改计划及全部关联配置。启用中的计划重新检查时间冲突并计算下次生成时间；
     * 停用中的计划保持无待生成时间，待重新启用时再校验配置。
     */
    @Override
    @Transactional
    @OperationLog(value = "修改巡检计划", businessType = "INSPECTION_PLAN")
    public void update(Long inspectionPlanId, InspectionPlanSaveDTO dto) {
        normalize(dto);
        validateDuplicateIds(dto.getEquipmentIds(), dto.getInspectionItemIds());
        validateSchedule(dto);

        LocalDateTime nextGenerateAt = calculateNextGenerateAt(dto);
        if (nextGenerateAt == null) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NO_FUTURE_EXECUTION);
        }

        InspectionPlan existing = requirePlanForUpdate(inspectionPlanId);
        List<Equipment> equipmentList = validateEquipment(dto.getEquipmentIds());
        validateInspector(dto.getInspectorId());
        List<InspectionItem> inspectionItems =
                validateInspectionItems(dto.getInspectionItemIds());
        validateEquipmentAndItems(equipmentList, inspectionItems);
        validateUniqueName(dto.getName(), inspectionPlanId);

        if (Integer.valueOf(1).equals(existing.getEnabled())) {
            checkScheduleConflict(dto, inspectionPlanId);
        }

        InspectionPlan plan = buildPlan(
                inspectionPlanId,
                dto,
                existing.getEnabled(),
                Integer.valueOf(1).equals(existing.getEnabled())
                        ? nextGenerateAt : null);
        try {
            inspectionPlanMapper.update(plan);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NAME_EXISTS);
        }

        replaceRelations(inspectionPlanId, dto);
    }

    /**
     * 停用会清除下次生成时间；重新启用时重新核对负责人、设备、项目、
     * 周期和计划冲突，避免沿用已失效的关联配置。
     */
    @Override
    @Transactional
    @OperationLog(value = "启停巡检计划", businessType = "INSPECTION_PLAN")
    public void changeStatus(
            Long inspectionPlanId,
            InspectionPlanStatusDTO statusDTO) {
        InspectionPlan plan = requirePlanForUpdate(inspectionPlanId);
        boolean targetEnabled = Boolean.TRUE.equals(statusDTO.getEnabled());
        if ((plan.getEnabled() == 1) == targetEnabled) {
            return;
        }

        if (!targetEnabled) {
            inspectionPlanMapper.updateStatus(
                    inspectionPlanId, false, null);
            return;
        }

        InspectionPlanSaveDTO dto = loadPlanConfiguration(plan);
        validateSchedule(dto);
        LocalDateTime nextGenerateAt = calculateNextGenerateAt(dto);
        if (nextGenerateAt == null) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NO_FUTURE_EXECUTION);
        }

        List<Equipment> equipmentList = validateEquipment(dto.getEquipmentIds());
        validateInspector(dto.getInspectorId());
        List<InspectionItem> inspectionItems =
                validateInspectionItems(dto.getInspectionItemIds());
        validateEquipmentAndItems(equipmentList, inspectionItems);
        checkScheduleConflict(dto, inspectionPlanId);

        inspectionPlanMapper.updateStatus(
                inspectionPlanId, true, nextGenerateAt);
    }

    /**
     * 只删除从未生成任务的计划，保留已有任务可追溯的计划来源。
     * 业务检查之后仍捕获外键约束异常，应对并发产生的新引用。
     */
    @Override
    @Transactional
    @OperationLog(value = "删除巡检计划", businessType = "INSPECTION_PLAN")
    public void delete(Long inspectionPlanId) {
        requirePlanForUpdate(inspectionPlanId);
        if (inspectionPlanMapper.countTasks(inspectionPlanId) > 0) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_HAS_TASKS);
        }
        try {
            inspectionPlanMapper.deleteById(inspectionPlanId);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_HAS_TASKS);
        }
    }


    /**
     * 校验巡检员：
     * 1. 用户存在
     * 2. 用户启用
     * 3. 角色必须为 INSPECTOR
     */
    private void validateInspector(Long inspectorId) {

        User user = userMapper.getById(inspectorId);

        if (user == null
                || !Integer.valueOf(1).equals(user.getEnabled())
                || !RoleCode.INSPECTOR.name().equals(user.getRoleCode())) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_INSPECTOR_UNAVAILABLE
            );
        }
    }

    /**
     * 校验设备：
     * 1. 所有设备都存在
     * 2. 状态必须为 NORMAL
     * 3. 所有设备必须属于同一个设备类型
     */
    private List<Equipment> validateEquipment(
            List<Long> equipmentIds) {

        List<Equipment> equipmentList =
                equipmentMapper.getByIds(equipmentIds);

        if (equipmentList.size()
                != equipmentIds.size()) {

            throw new BusinessException(
                    ErrorCode.EQUIPMENT_NOT_FOUND
            );
        }

        boolean hasInvalidEquipment =
                equipmentList.stream()
                        .anyMatch(equipment ->
                                !"NORMAL".equals(
                                        equipment.getStatus()
                                )
                        );

        if (hasInvalidEquipment) {
            throw new BusinessException(
                    ErrorCode.EQUIPMENT_STATUS_INVALID
            );
        }

        long equipmentTypeCount =
                equipmentList.stream()
                        .map(Equipment::getEquipmentTypeId)
                        .distinct()
                        .count();

        if (equipmentTypeCount > 1) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_EQUIPMENT_TYPE_MISMATCH
            );
        }

        return equipmentList;
    }

    /**
     * 校验巡检项目：
     * 1. 所有巡检项目都存在
     * 2. 必须全部启用
     */
    private List<InspectionItem> validateInspectionItems(
            List<Long> inspectionItemIds) {

        List<InspectionItem> items =
                inspectionItemMapper
                        .getByIds(inspectionItemIds);

        if (items.size()
                != inspectionItemIds.size()) {

            throw new BusinessException(
                    ErrorCode.INSPECTION_ITEM_NOT_FOUND
            );
        }

        boolean hasDisabledItem =
                items.stream()
                        .anyMatch(item ->
                                !Integer.valueOf(1)
                                        .equals(
                                                item.getEnabled()
                                        )
                        );

        if (hasDisabledItem) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_ITEM_DISABLED
            );
        }

        return items;
    }

    /**
     * 校验设备类型和巡检项目是否匹配
     */
    private void validateEquipmentAndItems(
            List<Equipment> equipmentList,
            List<InspectionItem> inspectionItems) {

        Long equipmentTypeId =
                equipmentList.get(0)
                        .getEquipmentTypeId();

        boolean hasInvalidItem =
                inspectionItems.stream()
                        .anyMatch(item ->
                                !equipmentTypeId.equals(
                                        item.getEquipmentTypeId()
                                )
                        );

        if (hasInvalidItem) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_ITEM_TYPE_MISMATCH
            );
        }
    }

    /**
     * 校验周期配置
     */
    private void validateSchedule(
            InspectionPlanSaveDTO dto) {

        if (dto.getDeadlineHours() == null
                || dto.getDeadlineHours() < 1
                || dto.getDeadlineHours() > 168) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_SCHEDULE_INVALID
            );
        }

        if (dto.getEndDate() != null
                && dto.getStartDate()
                .isAfter(dto.getEndDate())) {

            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_SCHEDULE_INVALID
            );
        }

        switch (dto.getFrequencyType()) {

            case DAILY -> {
                // DAILY 不需要周期明细
            }

            case WEEKLY -> {

                if (dto.getWeekDays() == null
                        || dto.getWeekDays()
                        .isEmpty()) {

                    throw new BusinessException(
                            ErrorCode.INSPECTION_PLAN_SCHEDULE_INVALID
                    );
                }

                boolean invalidWeekDay =
                        dto.getWeekDays()
                                .stream()
                                .anyMatch(day ->
                                        day == null
                                                || day < 1
                                                || day > 7
                                );

                if (invalidWeekDay || hasDuplicates(dto.getWeekDays())) {
                    throw new BusinessException(
                            ErrorCode.INSPECTION_PLAN_SCHEDULE_INVALID
                    );
                }
            }

            case MONTHLY -> {

                if (dto.getMonthDays() == null
                        || dto.getMonthDays()
                        .isEmpty()) {

                    throw new BusinessException(
                            ErrorCode.INSPECTION_PLAN_SCHEDULE_INVALID
                    );
                }

                boolean invalidMonthDay =
                        dto.getMonthDays()
                                .stream()
                                .anyMatch(day ->
                                        day == null
                                                || day < 1
                                                || day > 28
                                );

                if (invalidMonthDay || hasDuplicates(dto.getMonthDays())) {
                    throw new BusinessException(
                            ErrorCode.INSPECTION_PLAN_SCHEDULE_INVALID
                    );
                }
            }
        }
    }

    /**
     * 校验设备ID、巡检项目ID是否重复
     */
    private void validateDuplicateIds(
            List<Long> equipmentIds,
            List<Long> inspectionItemIds) {

        if (new HashSet<>(equipmentIds).size()
                != equipmentIds.size()) {

            throw new BusinessException(
                    ErrorCode.PARAM_VALIDATION_FAILED
            );
        }

        if (new HashSet<>(inspectionItemIds).size()
                != inspectionItemIds.size()) {

            throw new BusinessException(
                    ErrorCode.PARAM_VALIDATION_FAILED
            );
        }
    }

    private boolean hasDuplicates(List<Integer> values) {
        return new HashSet<>(values).size() != values.size();
    }

    private InspectionPlan requirePlanForUpdate(Long inspectionPlanId) {
        InspectionPlan plan =
                inspectionPlanMapper.getByIdForUpdate(inspectionPlanId);
        if (plan == null) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NOT_FOUND);
        }
        return plan;
    }

    private void validateUniqueName(String name, Long currentPlanId) {
        InspectionPlan sameName = inspectionPlanMapper.getByName(name);
        if (sameName != null && !sameName.getId().equals(currentPlanId)) {
            throw new BusinessException(
                    ErrorCode.INSPECTION_PLAN_NAME_EXISTS);
        }
    }

    private InspectionPlan buildPlan(
            Long id,
            InspectionPlanSaveDTO dto,
            Integer enabled,
            LocalDateTime nextGenerateAt) {
        InspectionPlan plan = new InspectionPlan();
        plan.setId(id);
        plan.setName(dto.getName());
        plan.setInspectorId(dto.getInspectorId());
        plan.setFrequencyType(dto.getFrequencyType().name());
        plan.setExecutionTime(dto.getExecutionTime());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setDeadlineHours(dto.getDeadlineHours());
        plan.setEnabled(enabled);
        plan.setNextGenerateAt(nextGenerateAt);
        plan.setRemark(dto.getRemark());
        return plan;
    }

    private void replaceRelations(
            Long inspectionPlanId,
            InspectionPlanSaveDTO dto) {
        inspectionPlanMapper.deleteScheduleRelations(inspectionPlanId);
        inspectionPlanMapper.deleteItemRelations(inspectionPlanId);
        inspectionPlanMapper.deleteEquipmentRelations(inspectionPlanId);

        inspectionPlanMapper.batchInsertEquipment(
                inspectionPlanId, dto.getEquipmentIds());
        inspectionPlanMapper.batchInsertItems(
                inspectionPlanId, dto.getInspectionItemIds());

        List<Integer> scheduleDays = switch (dto.getFrequencyType()) {
            case DAILY -> List.of();
            case WEEKLY -> dto.getWeekDays();
            case MONTHLY -> dto.getMonthDays();
        };
        if (!scheduleDays.isEmpty()) {
            inspectionPlanMapper.batchInsertSchedule(
                    inspectionPlanId, scheduleDays);
        }
    }

    private InspectionPlanSaveDTO loadPlanConfiguration(
            InspectionPlan plan) {
        InspectionPlanSaveDTO dto = new InspectionPlanSaveDTO();
        dto.setName(plan.getName());
        dto.setInspectorId(plan.getInspectorId());
        dto.setFrequencyType(FrequencyType.valueOf(plan.getFrequencyType()));
        dto.setExecutionTime(plan.getExecutionTime());
        dto.setStartDate(plan.getStartDate());
        dto.setEndDate(plan.getEndDate());
        dto.setDeadlineHours(plan.getDeadlineHours());
        dto.setRemark(plan.getRemark());

        List<Long> equipmentIds =
                inspectionPlanMapper.listEquipmentIdsByPlanId(plan.getId());
        List<Long> itemIds =
                inspectionPlanMapper.listItemIdsByPlanId(plan.getId());
        if (equipmentIds.isEmpty()) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        if (itemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INSPECTION_ITEM_NOT_FOUND);
        }
        dto.setEquipmentIds(equipmentIds);
        dto.setInspectionItemIds(itemIds);

        List<Integer> scheduleDays =
                inspectionPlanMapper.listScheduleDaysByPlanId(plan.getId());
        if (dto.getFrequencyType() == FrequencyType.WEEKLY) {
            dto.setWeekDays(scheduleDays);
        } else if (dto.getFrequencyType() == FrequencyType.MONTHLY) {
            dto.setMonthDays(scheduleDays);
        }
        return dto;
    }

    private void normalize(InspectionPlanSaveDTO dto) {
        dto.setName(dto.getName().trim());
        if (dto.getRemark() != null) {
            String remark = dto.getRemark().trim();
            dto.setRemark(remark.isEmpty() ? null : remark);
        }
    }

    /**
     * 计算下一次任务生成时间
     */
    private LocalDateTime calculateNextGenerateAt(
            InspectionPlanSaveDTO dto) {

        LocalDateTime now =
                LocalDateTime.now();

        LocalDate candidateDate =
                dto.getStartDate()
                        .isAfter(now.toLocalDate())
                        ? dto.getStartDate()
                        : now.toLocalDate();

        LocalDateTime candidate;

        switch (dto.getFrequencyType()) {

            case DAILY -> {

                candidate =
                        LocalDateTime.of(
                                candidateDate,
                                dto.getExecutionTime()
                        );

                if (candidate.isBefore(now)) {
                    candidate =
                            candidate.plusDays(1);
                }
            }

            case WEEKLY -> {

                while (true) {

                    if (dto.getEndDate() != null
                            && candidateDate
                            .isAfter(dto.getEndDate())) {

                        return null;
                    }

                    int weekDay =
                            candidateDate
                                    .getDayOfWeek()
                                    .getValue();

                    if (dto.getWeekDays()
                            .contains(weekDay)) {

                        candidate =
                                LocalDateTime.of(
                                        candidateDate,
                                        dto.getExecutionTime()
                                );

                        if (!candidate
                                .isBefore(now)) {

                            break;
                        }
                    }

                    candidateDate =
                            candidateDate.plusDays(1);
                }
            }

            case MONTHLY -> {

                while (true) {

                    if (dto.getEndDate() != null
                            && candidateDate
                            .isAfter(dto.getEndDate())) {

                        return null;
                    }

                    int monthDay =
                            candidateDate
                                    .getDayOfMonth();

                    if (dto.getMonthDays()
                            .contains(monthDay)) {

                        candidate =
                                LocalDateTime.of(
                                        candidateDate,
                                        dto.getExecutionTime()
                                );

                        if (!candidate
                                .isBefore(now)) {

                            break;
                        }
                    }

                    candidateDate =
                            candidateDate.plusDays(1);
                }
            }

            default ->
                    throw new BusinessException(
                            ErrorCode.PARAM_VALIDATION_FAILED
                    );
        }

        if (dto.getEndDate() != null
                && candidate
                .toLocalDate()
                .isAfter(dto.getEndDate())) {

            return null;
        }

        return candidate;
    }

    /**
     * 先在数据库按设备、执行时刻和日期区间筛候选，再比较周期实际相交日。
     * 仅频率类型相同或日期区间重叠不足以判定存在执行冲突。
     */
    private void checkScheduleConflict(
            InspectionPlanSaveDTO dto,
            Long excludePlanId) {

        List<InspectionPlanConflictVO> candidates =
                inspectionPlanMapper
                        .findConflictCandidates(
                                 dto.getEquipmentIds(),
                                 dto.getExecutionTime(),
                                 dto.getStartDate(),
                                 dto.getEndDate(),
                                 excludePlanId
                        );

        if (candidates.isEmpty()) {
            return;
        }

        List<Long> planIds =
                candidates.stream()
                        .map(
                                InspectionPlanConflictVO::getPlanId
                        )
                        .toList();

        List<InspectionPlanScheduleVO> schedules =
                inspectionPlanMapper
                        .findSchedulesByPlanIds(planIds);

        Map<Long, List<Integer>> scheduleMap =
                schedules.stream()
                        .collect(
                                Collectors.groupingBy(
                                        InspectionPlanScheduleVO::getPlanId,
                                        Collectors.mapping(
                                                InspectionPlanScheduleVO::getDayValue,
                                                Collectors.toList()
                                        )
                                )
                        );

        for (InspectionPlanConflictVO candidate
                : candidates) {

            List<Integer> oldDayValues =
                    scheduleMap.getOrDefault(
                            candidate.getPlanId(),
                            List.of()
                    );

            if (hasScheduleConflict(
                    dto,
                    candidate,
                    oldDayValues)) {

                throw new BusinessException(
                        ErrorCode.INSPECTION_PLAN_CONFLICT
                );
            }
        }
    }

    /**
     * 每日、每周和每月周期均在限定窗口内寻找共同执行日；
     * 搜索上限 400 天覆盖跨年的周/月组合，避免无结束日期时无限遍历。
     */
    private boolean hasScheduleConflict(
            InspectionPlanSaveDTO newPlan,
            InspectionPlanConflictVO oldPlan,
            List<Integer> oldDayValues) {

        LocalDate overlapStart =
                newPlan.getStartDate()
                        .isAfter(oldPlan.getStartDate())
                        ? newPlan.getStartDate()
                        : oldPlan.getStartDate();

        LocalDate overlapEnd =
                minEndDate(
                        newPlan.getEndDate(),
                        oldPlan.getEndDate()
                );

        LocalDate maxSearchEnd =
                overlapStart.plusDays(400);

        LocalDate searchEnd;

        if (overlapEnd == null
                || overlapEnd
                .isAfter(maxSearchEnd)) {

            searchEnd = maxSearchEnd;

        } else {

            searchEnd = overlapEnd;
        }

        LocalDate date =
                overlapStart;

        while (!date.isAfter(searchEnd)) {

            boolean newExecutes =
                    executesOn(
                            newPlan,
                            date
                    );

            boolean oldExecutes =
                    executesOn(
                            oldPlan.getFrequencyType(),
                            oldDayValues,
                            date
                    );

            if (newExecutes
                    && oldExecutes) {

                return true;
            }

            date =
                    date.plusDays(1);
        }

        return false;
    }

    /**
     * 判断新计划在指定日期是否执行
     */
    private boolean executesOn(
            InspectionPlanSaveDTO dto,
            LocalDate date) {

        return switch (dto.getFrequencyType()) {

            case DAILY ->
                    true;

            case WEEKLY ->
                    dto.getWeekDays()
                            .contains(
                                    date.getDayOfWeek()
                                            .getValue()
                            );

            case MONTHLY ->
                    dto.getMonthDays()
                            .contains(
                                    date.getDayOfMonth()
                            );
        };
    }

    /**
     * 判断旧计划在指定日期是否执行
     */
    private boolean executesOn(
            String frequencyType,
            List<Integer> dayValues,
            LocalDate date) {

        return switch (frequencyType) {

            case "DAILY" ->
                    true;

            case "WEEKLY" ->
                    dayValues.contains(
                            date.getDayOfWeek()
                                    .getValue()
                    );

            case "MONTHLY" ->
                    dayValues.contains(
                            date.getDayOfMonth()
                    );

            default ->
                    false;
        };
    }

    /**
     * 取两个结束日期中较早的一个
     *
     * null 代表长期有效
     */
    private LocalDate minEndDate(
            LocalDate end1,
            LocalDate end2) {

        if (end1 == null) {
            return end2;
        }

        if (end2 == null) {
            return end1;
        }

        return end1.isBefore(end2)
                ? end1
                : end2;
    }


    /**
     * 分页查询巡检计划
     */
    @Override
    public PageResult<InspectionPlanPageVO> pageQuery(
            InspectionPlanPageQueryDTO dto) {

        if (dto.getKeyword() != null) {
            String keyword = dto.getKeyword().trim();
            dto.setKeyword(keyword.isEmpty() ? null : keyword);
        }

        PageHelper.startPage(
                dto.getPageNum(),
                dto.getPageSize()
        );

        Page<InspectionPlanPageVO> page =
                inspectionPlanMapper.pageQuery(dto);

        return new PageResult<>(
                dto.getPageNum(),
                dto.getPageSize(),
                page.getTotal(),
                page.getResult()
        );
    }
}
