package com.zhixunyun.service.impl;

import com.zhixunyun.annotation.OperationLog;

import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhixunyun.dto.EquipmentTypeQueryDTO;
import com.zhixunyun.dto.EquipmentTypeSaveDTO;
import com.zhixunyun.dto.EquipmentTypeStatusDTO;
import com.zhixunyun.entity.EquipmentType;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.EquipmentTypeMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.EquipmentTypeService;
import com.zhixunyun.service.OptionCacheService;
import com.zhixunyun.vo.EquipmentTypeDetailVO;
import com.zhixunyun.vo.EquipmentTypeOptionVO;
import com.zhixunyun.vo.EquipmentTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/** 设备类型档案与无关键字选项缓存；停用前阻止仍有启用设备的类型失效。 */
@Service
@RequiredArgsConstructor
public class EquipmentTypeServiceImpl implements EquipmentTypeService {

    private final EquipmentTypeMapper equipmentTypeMapper;
    private final OptionCacheService optionCacheService;

    /**
     * 分页查询
     */
    @Override
    public PageResult<EquipmentTypeVO> pageQuery(EquipmentTypeQueryDTO dto) {

        dto.setKeyword(trimToNull(dto.getKeyword()));

        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());

        List<EquipmentTypeVO> list = equipmentTypeMapper.pageQuery(dto);
        PageInfo<EquipmentTypeVO> pageInfo = new PageInfo<>(list);

        return new PageResult<>(
                dto.getPageNum(),
                dto.getPageSize(),
                pageInfo.getTotal(),
                list
        );
    }

    /**
     * 新增设备类型。
     * 业务层先校验编码，数据库唯一索引负责并发场景下的最终兜底。
     */
    @Override
    @OperationLog(value = "新增设备类型", businessType = "EQUIPMENT_TYPE")
    public Long createEquipmentType(EquipmentTypeSaveDTO dto) {
        String code = dto.getCode().trim();

        if (equipmentTypeMapper.getByCode(code) != null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_CODE_EXISTS);
        }

        EquipmentType equipmentType = buildEntity(null, dto);
        equipmentType.setEnabled(1);

        try {
            equipmentTypeMapper.insert(equipmentType);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_CODE_EXISTS);
        }

        optionCacheService.evict(OptionCacheService.EQUIPMENT_TYPE_OPTIONS_KEY);

        return equipmentType.getId();
    }

    /**
     * 查询启用的设备类型，供设备新增、巡检项目配置等下拉框使用。
     */
    @Override
    public List<EquipmentTypeOptionVO> listOptions(String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword == null) {
            List<EquipmentTypeOptionVO> cached = optionCacheService.getList(
                    OptionCacheService.EQUIPMENT_TYPE_OPTIONS_KEY,
                    EquipmentTypeOptionVO.class);
            if (cached != null) {
                return cached;
            }
        }
        List<EquipmentTypeOptionVO> result =
                equipmentTypeMapper.listOptions(normalizedKeyword);
        if (normalizedKeyword == null) {
            optionCacheService.put(
                    OptionCacheService.EQUIPMENT_TYPE_OPTIONS_KEY, result);
        }
        return result;
    }

    @Override
    public EquipmentTypeDetailVO getEquipmentType(Long equipmentTypeId) {
        EquipmentTypeDetailVO detail = equipmentTypeMapper.getDetailById(equipmentTypeId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_NOT_FOUND);
        }
        return detail;
    }

    @Override
    @OperationLog(value = "修改设备类型", businessType = "EQUIPMENT_TYPE")
    public void updateEquipmentType(Long equipmentTypeId, EquipmentTypeSaveDTO dto) {
        if (equipmentTypeMapper.getById(equipmentTypeId) == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_NOT_FOUND);
        }

        String code = dto.getCode().trim();
        EquipmentType sameCodeType = equipmentTypeMapper.getByCode(code);
        if (sameCodeType != null && !sameCodeType.getId().equals(equipmentTypeId)) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_CODE_EXISTS);
        }

        try {
            equipmentTypeMapper.update(buildEntity(equipmentTypeId, dto));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_CODE_EXISTS);
        }
        optionCacheService.evict(OptionCacheService.EQUIPMENT_TYPE_OPTIONS_KEY);
    }

    @Override
    @OperationLog(value = "启停设备类型", businessType = "EQUIPMENT_TYPE")
    public void changeStatus(Long equipmentTypeId, EquipmentTypeStatusDTO dto) {
        EquipmentType equipmentType = equipmentTypeMapper.getById(equipmentTypeId);
        if (equipmentType == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_NOT_FOUND);
        }

        int targetEnabled = Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0;
        if (equipmentType.getEnabled() == targetEnabled) {
            return;
        }

        if (targetEnabled == 0
                && equipmentTypeMapper.countEnabledEquipment(equipmentTypeId) > 0) {
            throw new BusinessException(ErrorCode.EQUIPMENT_TYPE_HAS_ENABLED_EQUIPMENT);
        }

        equipmentTypeMapper.updateStatus(equipmentTypeId, targetEnabled == 1);
        optionCacheService.evict(OptionCacheService.EQUIPMENT_TYPE_OPTIONS_KEY);
    }

    private EquipmentType buildEntity(Long equipmentTypeId, EquipmentTypeSaveDTO dto) {
        EquipmentType equipmentType = new EquipmentType();
        equipmentType.setId(equipmentTypeId);
        equipmentType.setCode(dto.getCode().trim());
        equipmentType.setName(dto.getName().trim());
        equipmentType.setDescription(trimToNull(dto.getDescription()));
        return equipmentType;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
