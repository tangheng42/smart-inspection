package com.zhixunyun.service.impl;

import com.zhixunyun.dto.EquipmentTypeSaveDTO;
import com.zhixunyun.dto.EquipmentTypeStatusDTO;
import com.zhixunyun.entity.EquipmentType;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.EquipmentTypeMapper;
import com.zhixunyun.service.OptionCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentTypeServiceImplTest {

    @Mock
    private EquipmentTypeMapper equipmentTypeMapper;

    @Mock
    private OptionCacheService optionCacheService;

    @InjectMocks
    private EquipmentTypeServiceImpl equipmentTypeService;

    @Test
    void createShouldReturnGeneratedId() {
        EquipmentTypeSaveDTO dto = saveDto(" NEW_TYPE ", " 新设备类型 ");
        when(equipmentTypeMapper.getByCode("NEW_TYPE")).thenReturn(null);
        doAnswer(invocation -> {
            EquipmentType entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        }).when(equipmentTypeMapper).insert(any(EquipmentType.class));

        Long id = equipmentTypeService.createEquipmentType(dto);

        assertThat(id).isEqualTo(99L);
        verify(equipmentTypeMapper).insert(any(EquipmentType.class));
    }

    @Test
    void createShouldRejectDuplicateCode() {
        EquipmentTypeSaveDTO dto = saveDto("UPS", "UPS电源");
        when(equipmentTypeMapper.getByCode("UPS")).thenReturn(equipmentType(1L, 1));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> equipmentTypeService.createEquipmentType(dto))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.EQUIPMENT_TYPE_CODE_EXISTS));
        verify(equipmentTypeMapper, never()).insert(any());
    }

    @Test
    void disableShouldRejectTypeWithEnabledEquipment() {
        EquipmentTypeStatusDTO dto = new EquipmentTypeStatusDTO();
        dto.setEnabled(false);
        when(equipmentTypeMapper.getById(1L)).thenReturn(equipmentType(1L, 1));
        when(equipmentTypeMapper.countEnabledEquipment(1L)).thenReturn(2);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> equipmentTypeService.changeStatus(1L, dto))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.EQUIPMENT_TYPE_HAS_ENABLED_EQUIPMENT));
        verify(equipmentTypeMapper, never()).updateStatus(1L, false);
    }

    @Test
    void repeatedStatusChangeShouldBeIdempotent() {
        EquipmentTypeStatusDTO dto = new EquipmentTypeStatusDTO();
        dto.setEnabled(true);
        when(equipmentTypeMapper.getById(1L)).thenReturn(equipmentType(1L, 1));

        equipmentTypeService.changeStatus(1L, dto);

        verify(equipmentTypeMapper, never()).countEnabledEquipment(1L);
        verify(equipmentTypeMapper, never()).updateStatus(1L, true);
    }

    private EquipmentTypeSaveDTO saveDto(String code, String name) {
        EquipmentTypeSaveDTO dto = new EquipmentTypeSaveDTO();
        dto.setCode(code);
        dto.setName(name);
        dto.setDescription("测试说明");
        return dto;
    }

    private EquipmentType equipmentType(Long id, int enabled) {
        EquipmentType equipmentType = new EquipmentType();
        equipmentType.setId(id);
        equipmentType.setEnabled(enabled);
        return equipmentType;
    }
}
