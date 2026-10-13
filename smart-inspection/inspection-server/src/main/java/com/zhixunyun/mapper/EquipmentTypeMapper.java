package com.zhixunyun.mapper;

import java.util.List;

import com.zhixunyun.dto.EquipmentTypeQueryDTO;
import com.zhixunyun.entity.EquipmentType;
import com.zhixunyun.vo.EquipmentTypeDetailVO;
import com.zhixunyun.vo.EquipmentTypeOptionVO;
import com.zhixunyun.vo.EquipmentTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EquipmentTypeMapper {

    List<EquipmentTypeVO> pageQuery(EquipmentTypeQueryDTO dto);

    EquipmentType getByCode(String code);

    EquipmentType getById(Long equipmentTypeId);

    int insert(EquipmentType equipmentType);

    List<EquipmentTypeOptionVO> listOptions(String keyword);

    EquipmentTypeDetailVO getDetailById(Long equipmentTypeId);

    int update(EquipmentType equipmentType);

    int updateStatus(
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("enabled") boolean enabled);

    int countEnabledEquipment(Long equipmentTypeId);
}
