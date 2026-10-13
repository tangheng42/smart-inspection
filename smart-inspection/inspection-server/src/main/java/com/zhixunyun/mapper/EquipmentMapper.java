package com.zhixunyun.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.zhixunyun.dto.EquipmentQueryDTO;
import com.zhixunyun.entity.Equipment;
import com.zhixunyun.enumeration.EquipmentStatus;
import com.zhixunyun.vo.EquipmentDetailVO;
import com.zhixunyun.vo.EquipmentOptionVO;
import com.zhixunyun.vo.EquipmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EquipmentMapper {

    /**
     * 分页查询设备
     */
    List<EquipmentVO> pageQuery(EquipmentQueryDTO dto);

    /**
     * 根据设备编码查询设备
     */
    Equipment getByCode(String code);

    /**
     * 根据设备序列号查询设备
     */
    Equipment getBySerialNumber(String serialNumber);

    /**
     * 根据设备ID查询设备
     */
    Equipment getById(Long equipmentId);

    /**
     * 插入设备
     */
    int insert(Equipment equipment);

    /**
     * 查询设备选项
     */
    List<EquipmentOptionVO> listOptions(
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("areaId") Long areaId,
            @Param("keyword") String keyword);

    /**
     * 根据设备ID查询设备详情
     */
    EquipmentDetailVO getDetailById(Long equipmentId);

    /**
     * 更新设备
     */
    int update(Equipment equipment);

    /**
     * 更新设备状态
     */
    int updateStatus(
            @Param("equipmentId") Long equipmentId,
            @Param("status") EquipmentStatus status);

    /**
     * 统计设备业务引用数量
     */
    int countBusinessReferences(Long equipmentId);

    /**
     * 删除设备
     */
    int deleteById(Long equipmentId);
    /**
     * 根据设备ID列表查询设备
     */
    List<Equipment> getByIds(@Param("ids") List<Long> ids);

    /**
     * 保存本次巡检时间；异常时仅把正常设备切换为故障状态。
     */
    int updateAfterInspection(
            @Param("equipmentId") Long equipmentId,
            @Param("abnormal") boolean abnormal,
            @Param("inspectedAt") LocalDateTime inspectedAt);

    int markInRepair(Long equipmentId);

    int restoreNormalIfNoActiveWorkOrder(Long equipmentId);
}
