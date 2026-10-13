package com.zhixunyun.service.impl;

import java.math.BigDecimal;

import com.zhixunyun.dto.InspectionItemSaveDTO;
import com.zhixunyun.dto.InspectionItemStatusDTO;
import com.zhixunyun.entity.EquipmentType;
import com.zhixunyun.entity.InspectionItem;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.InspectionResultType;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.EquipmentTypeMapper;
import com.zhixunyun.mapper.InspectionItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionItemServiceImplTest {

    @Mock
    private InspectionItemMapper inspectionItemMapper;

    @Mock
    private EquipmentTypeMapper equipmentTypeMapper;

    @InjectMocks
    private InspectionItemServiceImpl inspectionItemService;

    @Test
    void createNumberItemShouldReturnGeneratedId() {
        InspectionItemSaveDTO dto = numberDto();
        when(equipmentTypeMapper.getById(1L)).thenReturn(enabledType());
        when(inspectionItemMapper.getByCode("VOLTAGE")).thenReturn(null);
        doAnswer(invocation -> {
            InspectionItem item = invocation.getArgument(0);
            item.setId(88L);
            return 1;
        }).when(inspectionItemMapper).insert(any(InspectionItem.class));

        Long id = inspectionItemService.createInspectionItem(dto);

        assertThat(id).isEqualTo(88L);
        ArgumentCaptor<InspectionItem> captor =
                ArgumentCaptor.forClass(InspectionItem.class);
        verify(inspectionItemMapper).insert(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("VOLTAGE");
        assertThat(captor.getValue().getResultType()).isEqualTo("NUMBER");
        assertThat(captor.getValue().getRequired()).isEqualTo(1);
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
    }

    @Test
    void nonNumberItemShouldRejectUnitOrThresholds() {
        InspectionItemSaveDTO dto = numberDto();
        dto.setResultType(InspectionResultType.BOOLEAN);
        when(equipmentTypeMapper.getById(1L)).thenReturn(enabledType());

        assertInvalidResultConfig(dto);
        verify(inspectionItemMapper, never()).insert(any());
    }

    @Test
    void numberItemShouldRejectMinGreaterThanMax() {
        InspectionItemSaveDTO dto = numberDto();
        dto.setMinValue(new BigDecimal("230"));
        dto.setMaxValue(new BigDecimal("210"));
        when(equipmentTypeMapper.getById(1L)).thenReturn(enabledType());

        assertInvalidResultConfig(dto);
        verify(inspectionItemMapper, never()).insert(any());
    }

    @Test
    void createShouldRejectDuplicateCode() {
        InspectionItemSaveDTO dto = numberDto();
        when(equipmentTypeMapper.getById(1L)).thenReturn(enabledType());
        InspectionItem existing = new InspectionItem();
        existing.setId(2L);
        when(inspectionItemMapper.getByCode("VOLTAGE")).thenReturn(existing);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> inspectionItemService.createInspectionItem(dto))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INSPECTION_ITEM_CODE_EXISTS));
        verify(inspectionItemMapper, never()).insert(any());
    }

    @Test
    void repeatedStatusChangeShouldBeIdempotent() {
        InspectionItem item = new InspectionItem();
        item.setId(1L);
        item.setEnabled(1);
        InspectionItemStatusDTO dto = new InspectionItemStatusDTO();
        dto.setEnabled(true);
        when(inspectionItemMapper.getById(1L)).thenReturn(item);

        inspectionItemService.changeStatus(1L, dto);

        verify(inspectionItemMapper, never()).updateStatus(anyLong(), any(Boolean.class));
    }

    private void assertInvalidResultConfig(InspectionItemSaveDTO dto) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> inspectionItemService.createInspectionItem(dto))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INSPECTION_ITEM_RESULT_CONFIG_INVALID));
    }

    private InspectionItemSaveDTO numberDto() {
        InspectionItemSaveDTO dto = new InspectionItemSaveDTO();
        dto.setCode(" VOLTAGE ");
        dto.setName(" 输出电压 ");
        dto.setEquipmentTypeId(1L);
        dto.setStandardDescription("输出电压正常");
        dto.setResultType(InspectionResultType.NUMBER);
        dto.setUnit("V");
        dto.setMinValue(new BigDecimal("210"));
        dto.setMaxValue(new BigDecimal("230"));
        dto.setRequired(true);
        return dto;
    }

    private EquipmentType enabledType() {
        EquipmentType type = new EquipmentType();
        type.setId(1L);
        type.setEnabled(1);
        return type;
    }
}
