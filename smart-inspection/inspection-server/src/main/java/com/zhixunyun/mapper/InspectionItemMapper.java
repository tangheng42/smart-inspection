package com.zhixunyun.mapper;

import java.util.List;

import com.zhixunyun.dto.InspectionItemQueryDTO;
import com.zhixunyun.entity.InspectionItem;
import com.zhixunyun.vo.InspectionItemDetailVO;
import com.zhixunyun.vo.InspectionItemOptionVO;
import com.zhixunyun.vo.InspectionItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InspectionItemMapper {

    /**
     * 分页查询巡检项目
     */
    List<InspectionItemVO> pageQuery(InspectionItemQueryDTO dto);

    /**
     * 根据编码查询巡检项目
     */
    InspectionItem getByCode(String code);

    /**
     * 根据ID查询巡检项目
     */
    InspectionItem getById(Long inspectionItemId);

    /**
     * 新增巡检项目
     */
    int insert(InspectionItem inspectionItem);

    /**
     * 查询巡检项目选项
     */
    List<InspectionItemOptionVO> listOptions(
            @Param("equipmentTypeId") Long equipmentTypeId,
            @Param("keyword") String keyword);

    /**
     * 根据ID查询巡检项目详情
     */
    InspectionItemDetailVO getDetailById(Long inspectionItemId);

    /**
     * 更新巡检项目
     */
    int update(InspectionItem inspectionItem);

    /**
     * 更新巡检项目状态
     */
    int updateStatus(
            @Param("inspectionItemId") Long inspectionItemId,
            @Param("enabled") boolean enabled);

    /**
     * 根据ID列表查询巡检项目
     */
    List<InspectionItem> getByIds(@Param("ids") List<Long> ids);
}
