package com.zhixunyun.service;

import com.zhixunyun.dto.InspectionPlanPageQueryDTO;
import com.zhixunyun.dto.InspectionPlanSaveDTO;
import com.zhixunyun.dto.InspectionPlanStatusDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.InspectionPlanDetailVO;
import com.zhixunyun.vo.InspectionPlanPageVO;
import jakarta.validation.Valid;

public interface InspectionPlanService {
    /**
     * 创建巡检计划
     */
    Long create(@Valid InspectionPlanSaveDTO dto);

    /**
     * 分页查询巡检计划
     */
    PageResult<InspectionPlanPageVO> pageQuery(InspectionPlanPageQueryDTO dto);

    /**
     * 查询巡检计划详情
     */
    InspectionPlanDetailVO getById(Long inspectionPlanId);

    /**
     * 修改巡检计划
     */
    void update(Long inspectionPlanId, InspectionPlanSaveDTO dto);

    /**
     * 启用或停用巡检计划
     */
    void changeStatus(Long inspectionPlanId, InspectionPlanStatusDTO dto);

    /**
     * 删除从未生成任务的巡检计划
     */
    void delete(Long inspectionPlanId);
}
