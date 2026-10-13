package com.zhixunyun.service.impl;

import com.zhixunyun.annotation.OperationLog;

import java.math.BigDecimal;
import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhixunyun.dto.InspectionItemQueryDTO;
import com.zhixunyun.dto.InspectionItemSaveDTO;
import com.zhixunyun.dto.InspectionItemStatusDTO;
import com.zhixunyun.entity.EquipmentType;
import com.zhixunyun.entity.InspectionItem;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.InspectionResultType;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.EquipmentTypeMapper;
import com.zhixunyun.mapper.InspectionItemMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.InspectionItemService;
import com.zhixunyun.vo.InspectionItemDetailVO;
import com.zhixunyun.vo.InspectionItemOptionVO;
import com.zhixunyun.vo.InspectionItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/** 维护巡检项目及其结果类型配置，供计划配置和任务生成使用。 */
@Service
@RequiredArgsConstructor
public class InspectionItemServiceImpl implements InspectionItemService {

    private final InspectionItemMapper inspectionItemMapper;
    private final EquipmentTypeMapper equipmentTypeMapper;

    @Override
    public PageResult<InspectionItemVO> pageInspectionItems(InspectionItemQueryDTO dto) {
        dto.setKeyword(trimToNull(dto.getKeyword()));
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<InspectionItemVO> list = inspectionItemMapper.pageQuery(dto);
        PageInfo<InspectionItemVO> pageInfo = new PageInfo<>(list);
        return new PageResult<>(dto.getPageNum(), dto.getPageSize(), pageInfo.getTotal(), list);
    }

    @Override
    @OperationLog(value = "新增巡检项目", businessType = "INSPECTION_ITEM")
    public Long createInspectionItem(InspectionItemSaveDTO dto) {
        validateEquipmentType(dto.getEquipmentTypeId());
        validateResultConfig(dto);
        validateUnique(dto.getCode().trim(), null);

        InspectionItem item = buildEntity(null, dto);
        item.setEnabled(1);
        try {
            inspectionItemMapper.insert(item);
        } catch (DuplicateKeyException ex) {
            validateUnique(item.getCode(), null);
            throw ex;
        }
        return item.getId();
    }

    @Override
    public List<InspectionItemOptionVO> listOptions(
            Long equipmentTypeId, String keyword) {
        return inspectionItemMapper.listOptions(equipmentTypeId, trimToNull(keyword));
    }

    @Override
    public InspectionItemDetailVO getInspectionItem(Long inspectionItemId) {
        InspectionItemDetailVO detail = inspectionItemMapper.getDetailById(inspectionItemId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.INSPECTION_ITEM_NOT_FOUND);
        }
        return detail;
    }

    @Override
    @OperationLog(value = "修改巡检项目", businessType = "INSPECTION_ITEM")
    public void updateInspectionItem(Long inspectionItemId, InspectionItemSaveDTO dto) {
        if (inspectionItemMapper.getById(inspectionItemId) == null) {
            throw new BusinessException(ErrorCode.INSPECTION_ITEM_NOT_FOUND);
        }
        validateEquipmentType(dto.getEquipmentTypeId());
        validateResultConfig(dto);
        validateUnique(dto.getCode().trim(), inspectionItemId);

        InspectionItem item = buildEntity(inspectionItemId, dto);
        try {
            inspectionItemMapper.update(item);
        } catch (DuplicateKeyException ex) {
            validateUnique(item.getCode(), inspectionItemId);
            throw ex;
        }
    }

    @Override
    @OperationLog(value = "启停巡检项目", businessType = "INSPECTION_ITEM")
    public void changeStatus(Long inspectionItemId, InspectionItemStatusDTO dto) {
        InspectionItem item = inspectionItemMapper.getById(inspectionItemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.INSPECTION_ITEM_NOT_FOUND);
        }
        int targetEnabled = Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0;
        if (item.getEnabled() == targetEnabled) {
            return;
        }
        inspectionItemMapper.updateStatus(inspectionItemId, targetEnabled == 1);
    }

    private void validateEquipmentType(Long equipmentTypeId) {
        EquipmentType type = equipmentTypeMapper.getById(equipmentTypeId);
        if (type == null || type.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.INSPECTION_ITEM_TYPE_UNAVAILABLE);
        }
    }

    private void validateResultConfig(InspectionItemSaveDTO dto) {
        BigDecimal min = dto.getMinValue();
        BigDecimal max = dto.getMaxValue();
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(ErrorCode.INSPECTION_ITEM_RESULT_CONFIG_INVALID);
        }

        boolean hasNumberConfig = trimToNull(dto.getUnit()) != null
                || min != null
                || max != null;
        if (dto.getResultType() != InspectionResultType.NUMBER && hasNumberConfig) {
            throw new BusinessException(ErrorCode.INSPECTION_ITEM_RESULT_CONFIG_INVALID);
        }
    }

    private void validateUnique(String code, Long currentId) {
        InspectionItem sameCode = inspectionItemMapper.getByCode(code);
        if (sameCode != null
                && (currentId == null || !sameCode.getId().equals(currentId))) {
            throw new BusinessException(ErrorCode.INSPECTION_ITEM_CODE_EXISTS);
        }
    }

    private InspectionItem buildEntity(Long id, InspectionItemSaveDTO dto) {
        InspectionItem item = new InspectionItem();
        item.setId(id);
        item.setCode(dto.getCode().trim());
        item.setName(dto.getName().trim());
        item.setEquipmentTypeId(dto.getEquipmentTypeId());
        item.setCheckMethod(trimToNull(dto.getCheckMethod()));
        item.setStandardDescription(dto.getStandardDescription().trim());
        item.setResultType(dto.getResultType().name());
        item.setUnit(trimToNull(dto.getUnit()));
        item.setMinValue(dto.getMinValue());
        item.setMaxValue(dto.getMaxValue());
        item.setRequired(Boolean.TRUE.equals(dto.getRequired()) ? 1 : 0);
        return item;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
