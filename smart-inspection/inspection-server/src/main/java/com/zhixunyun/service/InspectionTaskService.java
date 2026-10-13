package com.zhixunyun.service;

import com.zhixunyun.dto.InspectionTaskPageQueryDTO;
import com.zhixunyun.dto.InspectionTaskSubmitRequest;
import com.zhixunyun.dto.InspectionTaskTodayQueryDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.InspectionTaskDetailVO;
import com.zhixunyun.vo.InspectionTaskPageVO;
import com.zhixunyun.vo.InspectionSubmitDataVO;

public interface InspectionTaskService {

    /**
     * 分页查询巡检任务
     */
    PageResult<InspectionTaskPageVO> pageQuery(InspectionTaskPageQueryDTO dto);

    /**
     * 分页查询今日巡检任务
     */
    PageResult<InspectionTaskPageVO> pageToday(InspectionTaskTodayQueryDTO dto);

    /**
     * 查询巡检任务详情
     */
    InspectionTaskDetailVO getById(Long inspectionTaskId);

    /**
     * 启动巡检任务
     */
    void start(Long inspectionTaskId);

    /** 取消尚未开始的巡检任务。 */
    void cancel(Long inspectionTaskId, String reason);

    InspectionSubmitDataVO submit(Long inspectionTaskId, InspectionTaskSubmitRequest request);
}
