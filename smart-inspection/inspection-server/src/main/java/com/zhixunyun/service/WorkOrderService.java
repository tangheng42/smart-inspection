package com.zhixunyun.service;

import java.util.List;

import com.zhixunyun.dto.WorkOrderAssignRequest;
import com.zhixunyun.dto.WorkOrderQueryDTO;
import com.zhixunyun.dto.WorkOrderResultSubmitRequest;
import com.zhixunyun.dto.WorkOrderVerifyRequest;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.WorkOrderDetailVO;
import com.zhixunyun.vo.WorkOrderOperationVO;
import com.zhixunyun.vo.WorkOrderVO;

public interface WorkOrderService {
    // 工单分页查询
    PageResult<WorkOrderVO> pageQuery(WorkOrderQueryDTO dto);
    // 工单详情
    WorkOrderDetailVO getById(Long workOrderId);
    // 工单操作记录
    List<WorkOrderOperationVO> listOperations(Long workOrderId);

    // 分配工单
    void assign(Long workOrderId, WorkOrderAssignRequest request);
    void accept(Long workOrderId);
    void submitResult(Long workOrderId, WorkOrderResultSubmitRequest request);
    void verify(Long workOrderId, WorkOrderVerifyRequest request);
    void rework(Long workOrderId, String remark);
    void cancel(Long workOrderId, String reason);
}
