package com.zhixunyun.service;

import java.util.List;

import com.zhixunyun.dto.AreaCreateDTO;
import com.zhixunyun.dto.AreaQueryDTO;
import com.zhixunyun.dto.AreaStatusDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.AreaDetailVO;
import com.zhixunyun.vo.AreaOptionVO;
import com.zhixunyun.vo.AreaVO;
import jakarta.validation.Valid;

public interface AreaService {

    /**
     * 分页查询区域
     */
    PageResult<AreaVO> pageAreas(AreaQueryDTO dto);

    /**
     * 新增区域
     */
    Long createArea(@Valid AreaCreateDTO dto);

    /**
     * 查询启用区域选项
     */
    List<AreaOptionVO> listAreaOptions(String keyword);

    /**
     * 查询区域详情
     */
    AreaDetailVO getArea(Long areaId);

    /**
     * 修改区域
     */
    void updateArea(Long areaId, @Valid AreaCreateDTO dto);

    /**
     * 启用或停用区域
     */
    void changeAreaStatus(Long areaId, @Valid AreaStatusDTO dto);
}
