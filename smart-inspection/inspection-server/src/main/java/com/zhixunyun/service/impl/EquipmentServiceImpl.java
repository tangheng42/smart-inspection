package com.zhixunyun.service.impl;

import com.zhixunyun.annotation.OperationLog;

import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhixunyun.dto.EquipmentQueryDTO;
import com.zhixunyun.dto.EquipmentSaveDTO;
import com.zhixunyun.dto.EquipmentStatusChangeDTO;
import com.zhixunyun.entity.Area;
import com.zhixunyun.entity.Equipment;
import com.zhixunyun.entity.EquipmentType;
import com.zhixunyun.enumeration.EquipmentStatus;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.AreaMapper;
import com.zhixunyun.mapper.EquipmentMapper;
import com.zhixunyun.mapper.EquipmentTypeMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.EquipmentService;
import com.zhixunyun.vo.EquipmentDetailVO;
import com.zhixunyun.vo.EquipmentOptionVO;
import com.zhixunyun.vo.EquipmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/** 设备档案维护；人工状态切换与巡检、维修产生的故障状态分别受约束。 */
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentTypeMapper equipmentTypeMapper;
    private final AreaMapper areaMapper;

    /**
     * 分页查询设备
     */
    @Override
    public PageResult<EquipmentVO> pageEquipment(EquipmentQueryDTO dto) {
        dto.setKeyword(trimToNull(dto.getKeyword()));
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<EquipmentVO> list = equipmentMapper.pageQuery(dto);
        PageInfo<EquipmentVO> pageInfo = new PageInfo<>(list);
        return new PageResult<>(dto.getPageNum(), dto.getPageSize(), pageInfo.getTotal(), list);
    }

    /**
     * 新增设备
     */
    @Override
    @OperationLog(value = "新增设备", businessType = "EQUIPMENT")
    public Long createEquipment(EquipmentSaveDTO dto) {
        validateLocation(dto.getEquipmentTypeId(), dto.getAreaId());
        validateUnique(dto.getCode().trim(), trimToNull(dto.getSerialNumber()), null);

        Equipment equipment = buildEntity(null, dto);
        equipment.setStatus(EquipmentStatus.NORMAL.name());
        try {
            equipmentMapper.insert(equipment);
        } catch (DuplicateKeyException ex) {
            validateUnique(equipment.getCode(), equipment.getSerialNumber(), null);
            throw ex;
        }
        return equipment.getId();
    }

    /**
     * 查询启用设备选项
     */
    @Override
    public List<EquipmentOptionVO> listOptions(
            Long equipmentTypeId, Long areaId, String keyword) {
        return equipmentMapper.listOptions(
                equipmentTypeId, areaId, trimToNull(keyword));
    }

    /**
     * 根据ID查询设备详情
     */
    @Override
    public EquipmentDetailVO getEquipment(Long equipmentId) {
        EquipmentDetailVO detail = equipmentMapper.getDetailById(equipmentId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        return detail;
    }

    /**
     * 更新设备
     */
    @Override
    @OperationLog(value = "修改设备", businessType = "EQUIPMENT")
    public void updateEquipment(Long equipmentId, EquipmentSaveDTO dto) {
        if (equipmentMapper.getById(equipmentId) == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        validateLocation(dto.getEquipmentTypeId(), dto.getAreaId());
        validateUnique(dto.getCode().trim(), trimToNull(dto.getSerialNumber()), equipmentId);

        Equipment equipment = buildEntity(equipmentId, dto);
        try {
            equipmentMapper.update(equipment);
        } catch (DuplicateKeyException ex) {
            validateUnique(equipment.getCode(), equipment.getSerialNumber(), equipmentId);
            throw ex;
        }
    }
    /**
     * 管理员只可手工切换正常与停用；故障和维修中状态由业务流程控制。
     * SQL 再限制旧状态，避免并发故障上报被人工操作覆盖。
     */
    @Override
    @OperationLog(value = "修改设备状态", businessType = "EQUIPMENT")
    public void changeStatus(Long equipmentId, EquipmentStatusChangeDTO dto) {
        Equipment equipment = equipmentMapper.getById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }

        EquipmentStatus target = dto.getStatus();
        if (target != EquipmentStatus.NORMAL && target != EquipmentStatus.DISABLED) {
            throw new BusinessException(ErrorCode.EQUIPMENT_STATUS_CHANGE_FORBIDDEN);
        }

        EquipmentStatus current = EquipmentStatus.valueOf(equipment.getStatus());
        if (current == target) {
            return;
        }
        if (current == EquipmentStatus.FAULT || current == EquipmentStatus.IN_REPAIR) {
            throw new BusinessException(ErrorCode.EQUIPMENT_STATUS_CHANGE_FORBIDDEN);
        }

        // 操作原因随请求参数进入操作日志，不写入设备档案字段。
        // SQL 再校验一次当前状态，避免并发故障上报后被管理操作覆盖。
        if (equipmentMapper.updateStatus(equipmentId, target) == 0) {
            throw new BusinessException(ErrorCode.EQUIPMENT_STATUS_CHANGE_FORBIDDEN);
        }
    }

    /**
     * 删除设备
     */
    @Override
    @OperationLog(value = "删除设备", businessType = "EQUIPMENT")
    public void deleteEquipment(Long equipmentId) {
        if (equipmentMapper.getById(equipmentId) == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        if (equipmentMapper.countBusinessReferences(equipmentId) > 0) {
            throw new BusinessException(ErrorCode.EQUIPMENT_REFERENCED);
        }
        try {
            equipmentMapper.deleteById(equipmentId);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.EQUIPMENT_REFERENCED);
        }
    }

    private void validateLocation(Long equipmentTypeId, Long areaId) {
        EquipmentType type = equipmentTypeMapper.getById(equipmentTypeId);
        if (type == null || type.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_UNAVAILABLE);
        }
        Area area = areaMapper.getById(areaId);
        if (area == null || area.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.EQUIPMENT_AREA_UNAVAILABLE);
        }
    }

    private void validateUnique(String code, String serialNumber, Long currentId) {
        Equipment sameCode = equipmentMapper.getByCode(code);
        if (isAnother(sameCode, currentId)) {
            throw new BusinessException(ErrorCode.EQUIPMENT_CODE_EXISTS);
        }
        if (serialNumber != null) {
            Equipment sameSerial = equipmentMapper.getBySerialNumber(serialNumber);
            if (isAnother(sameSerial, currentId)) {
                throw new BusinessException(ErrorCode.EQUIPMENT_SERIAL_NUMBER_EXISTS);
            }
        }
    }

    private boolean isAnother(Equipment equipment, Long currentId) {
        return equipment != null
                && (currentId == null || !equipment.getId().equals(currentId));
    }

    private Equipment buildEntity(Long id, EquipmentSaveDTO dto) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setCode(dto.getCode().trim());
        equipment.setName(dto.getName().trim());
        equipment.setEquipmentTypeId(dto.getEquipmentTypeId());
        equipment.setAreaId(dto.getAreaId());
        equipment.setModel(trimToNull(dto.getModel()));
        equipment.setManufacturer(trimToNull(dto.getManufacturer()));
        equipment.setSerialNumber(trimToNull(dto.getSerialNumber()));
        equipment.setInstallDate(dto.getInstallDate());
        equipment.setRemark(trimToNull(dto.getRemark()));
        return equipment;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
