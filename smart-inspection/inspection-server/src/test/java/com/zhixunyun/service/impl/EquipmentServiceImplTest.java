package com.zhixunyun.service.impl;

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
class EquipmentServiceImplTest {

    @Mock
    private EquipmentMapper equipmentMapper;

    @Mock
    private EquipmentTypeMapper equipmentTypeMapper;

    @Mock
    private AreaMapper areaMapper;

    @InjectMocks
    private EquipmentServiceImpl equipmentService;

    @Test
    void createShouldValidateReferencesTrimFieldsAndReturnGeneratedId() {
        EquipmentSaveDTO dto = saveDto();
        when(equipmentTypeMapper.getById(1L)).thenReturn(enabledType());
        when(areaMapper.getById(2L)).thenReturn(enabledArea());
        when(equipmentMapper.getByCode("EQ-TEST")).thenReturn(null);
        when(equipmentMapper.getBySerialNumber("SN-TEST")).thenReturn(null);
        doAnswer(invocation -> {
            Equipment equipment = invocation.getArgument(0);
            equipment.setId(99L);
            return 1;
        }).when(equipmentMapper).insert(any(Equipment.class));

        Long id = equipmentService.createEquipment(dto);

        assertThat(id).isEqualTo(99L);
        ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("EQ-TEST");
        assertThat(captor.getValue().getName()).isEqualTo("测试设备");
        assertThat(captor.getValue().getStatus()).isEqualTo(EquipmentStatus.NORMAL.name());
    }

    @Test
    void createShouldRejectDuplicateCode() {
        EquipmentSaveDTO dto = saveDto();
        when(equipmentTypeMapper.getById(1L)).thenReturn(enabledType());
        when(areaMapper.getById(2L)).thenReturn(enabledArea());
        when(equipmentMapper.getByCode("EQ-TEST")).thenReturn(equipment(8L, "NORMAL"));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> equipmentService.createEquipment(dto))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.EQUIPMENT_CODE_EXISTS));
        verify(equipmentMapper, never()).insert(any());
    }

    @Test
    void managementStatusEndpointShouldRejectBusinessStatus() {
        EquipmentStatusChangeDTO dto = new EquipmentStatusChangeDTO();
        dto.setStatus(EquipmentStatus.FAULT);
        when(equipmentMapper.getById(1L)).thenReturn(equipment(1L, "NORMAL"));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> equipmentService.changeStatus(1L, dto))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.EQUIPMENT_STATUS_CHANGE_FORBIDDEN));
        verify(equipmentMapper, never()).updateStatus(anyLong(), any());
    }

    @Test
    void deleteShouldRejectReferencedEquipment() {
        when(equipmentMapper.getById(1L)).thenReturn(equipment(1L, "NORMAL"));
        when(equipmentMapper.countBusinessReferences(1L)).thenReturn(2);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> equipmentService.deleteEquipment(1L))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.EQUIPMENT_REFERENCED));
        verify(equipmentMapper, never()).deleteById(anyLong());
    }

    @Test
    void statusChangeShouldRejectConcurrentBusinessTransition() {
        EquipmentStatusChangeDTO dto = new EquipmentStatusChangeDTO();
        dto.setStatus(EquipmentStatus.DISABLED);
        when(equipmentMapper.getById(1L)).thenReturn(equipment(1L, "NORMAL"));
        when(equipmentMapper.updateStatus(1L, EquipmentStatus.DISABLED)).thenReturn(0);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> equipmentService.changeStatus(1L, dto))
                .satisfies(exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.EQUIPMENT_STATUS_CHANGE_FORBIDDEN));
    }

    private EquipmentSaveDTO saveDto() {
        EquipmentSaveDTO dto = new EquipmentSaveDTO();
        dto.setCode(" EQ-TEST ");
        dto.setName(" 测试设备 ");
        dto.setEquipmentTypeId(1L);
        dto.setAreaId(2L);
        dto.setSerialNumber(" SN-TEST ");
        return dto;
    }

    private EquipmentType enabledType() {
        EquipmentType type = new EquipmentType();
        type.setId(1L);
        type.setEnabled(1);
        return type;
    }

    private Area enabledArea() {
        Area area = new Area();
        area.setId(2L);
        area.setEnabled(1);
        return area;
    }

    private Equipment equipment(Long id, String status) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setStatus(status);
        return equipment;
    }
}
