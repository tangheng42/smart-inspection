package com.zhixunyun.mapper;

import java.util.List;

import com.zhixunyun.dto.AreaQueryDTO;
import com.zhixunyun.entity.Area;
import com.zhixunyun.vo.AreaDetailVO;
import com.zhixunyun.vo.AreaVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AreaMapper {

    /**
     * 分页查询区域
     */
    List<AreaVO> pageQuery(AreaQueryDTO dto);

    /**
     * 根据编码查询区域
     */
    Area getByCode(String code);

    /**
     * 新增区域
     */
    void insert(Area area);

    /**
     * 查询启用区域选项
     */
    List<Area> getAreaOptions(String keyword);

    /**
     * 根据ID查询区域区域详情
     */
    AreaDetailVO getDetailById(Long areaId);

    /**
     * 根据ID查询区域
     */
    Area getById(Long areaId);

    /**
     * 更新区域
     */
    @Update("""
    update area
    set code = #{code},
        name = #{name},
        description = #{description}
    where id = #{id}
    """)
    void update(Area area);

    /**
     * 查询区域下设备数量
     */
    @Select("""
    select count(*)
    from equipment
    where area_id = #{areaId}
    """)
    int countEquipmentByAreaId(Long areaId);
}
