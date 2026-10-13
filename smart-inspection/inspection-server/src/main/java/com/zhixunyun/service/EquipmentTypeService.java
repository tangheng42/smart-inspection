package com.zhixunyun.service;

import java.util.List;

import com.zhixunyun.dto.EquipmentTypeQueryDTO;
import com.zhixunyun.dto.EquipmentTypeSaveDTO;
import com.zhixunyun.dto.EquipmentTypeStatusDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.EquipmentTypeDetailVO;
import com.zhixunyun.vo.EquipmentTypeOptionVO;
import com.zhixunyun.vo.EquipmentTypeVO;


public interface EquipmentTypeService {

    /**
     * 分页查询
     * @param dto
     * @return
     */
    PageResult<EquipmentTypeVO> pageQuery(EquipmentTypeQueryDTO dto);

    Long createEquipmentType(EquipmentTypeSaveDTO dto);

    List<EquipmentTypeOptionVO> listOptions(String keyword);

    EquipmentTypeDetailVO getEquipmentType(Long equipmentTypeId);

    void updateEquipmentType(Long equipmentTypeId, EquipmentTypeSaveDTO dto);

    void changeStatus(Long equipmentTypeId, EquipmentTypeStatusDTO dto);
}
