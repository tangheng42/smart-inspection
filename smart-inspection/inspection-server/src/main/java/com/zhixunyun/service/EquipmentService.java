package com.zhixunyun.service;

import java.util.List;

import com.zhixunyun.dto.EquipmentQueryDTO;
import com.zhixunyun.dto.EquipmentSaveDTO;
import com.zhixunyun.dto.EquipmentStatusChangeDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.EquipmentDetailVO;
import com.zhixunyun.vo.EquipmentOptionVO;
import com.zhixunyun.vo.EquipmentVO;

public interface EquipmentService {

    PageResult<EquipmentVO> pageEquipment(EquipmentQueryDTO dto);

    Long createEquipment(EquipmentSaveDTO dto);

    List<EquipmentOptionVO> listOptions(
            Long equipmentTypeId, Long areaId, String keyword);

    EquipmentDetailVO getEquipment(Long equipmentId);

    void updateEquipment(Long equipmentId, EquipmentSaveDTO dto);

    void changeStatus(Long equipmentId, EquipmentStatusChangeDTO dto);

    void deleteEquipment(Long equipmentId);
}
