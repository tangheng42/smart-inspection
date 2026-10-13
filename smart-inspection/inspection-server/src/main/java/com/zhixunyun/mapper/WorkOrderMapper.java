package com.zhixunyun.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.zhixunyun.dto.WorkOrderQueryDTO;
import com.zhixunyun.entity.WorkOrder;
import com.zhixunyun.entity.WorkOrderOperation;
import com.zhixunyun.enumeration.WorkOrderStatus;
import com.zhixunyun.vo.WorkOrderDetailVO;
import com.zhixunyun.vo.WorkOrderOperationVO;
import com.zhixunyun.vo.WorkOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkOrderMapper {
    //分页查询工单
    List<WorkOrderVO> pageQuery(WorkOrderQueryDTO dto);
    //根据id查询工单详情
    WorkOrderDetailVO getDetailById(Long workOrderId);
    //根据工单id查询工单优先级
    Long getMaintainerId(Long workOrderId);
    List<String> listFaultImageUrls(Long workOrderId);
    List<String> listResultImageUrls(Long workOrderId);
    //根据工单id查询工单操作
    List<WorkOrderOperationVO> listOperations(Long workOrderId);
    //根据id查询工单是否存在
    boolean existsById(@Param("workOrderId") Long workOrderId);
    int insert(WorkOrder workOrder);
    //插入工单操作
    int insertOperation(WorkOrderOperation operation);
    //根据id查询工单
    WorkOrder getById(@Param("workOrderId") Long workOrderId);
    //分配工单
    int assign(
            @Param("workOrderId") Long workOrderId,
            @Param("maintainerId") Long maintainerId,
            @Param("dueAt") LocalDateTime dueAt,
            @Param("assignedAt") LocalDateTime assignedAt,
            @Param("currentStatus") WorkOrderStatus currentStatus,
            @Param("version") Integer version
    );

    int accept(@Param("workOrderId") Long workOrderId,
               @Param("maintainerId") Long maintainerId,
               @Param("acceptedAt") LocalDateTime acceptedAt,
               @Param("version") Integer version);

    int submitResult(@Param("workOrderId") Long workOrderId,
                     @Param("maintainerId") Long maintainerId,
                     @Param("solution") String solution,
                     @Param("resultDescription") String resultDescription,
                     @Param("submittedAt") LocalDateTime submittedAt,
                     @Param("version") Integer version);

    int verify(@Param("workOrderId") Long workOrderId,
               @Param("toStatus") WorkOrderStatus toStatus,
               @Param("verifierId") Long verifierId,
               @Param("passed") boolean passed,
               @Param("remark") String remark,
               @Param("verifiedAt") LocalDateTime verifiedAt,
               @Param("version") Integer version);

    int rework(@Param("workOrderId") Long workOrderId,
               @Param("maintainerId") Long maintainerId,
               @Param("version") Integer version);

    int cancel(@Param("workOrderId") Long workOrderId,
               @Param("reason") String reason,
               @Param("cancelledAt") LocalDateTime cancelledAt,
               @Param("version") Integer version);
}
