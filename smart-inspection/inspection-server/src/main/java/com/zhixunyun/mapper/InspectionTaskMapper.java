package com.zhixunyun.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.zhixunyun.dto.InspectionTaskPageQueryDTO;
import com.zhixunyun.enumeration.InspectionTaskStatus;
import com.zhixunyun.vo.InspectionTaskDetailVO;
import com.zhixunyun.vo.InspectionTaskPageVO;
import com.zhixunyun.vo.InspectionTaskStartInfo;
import com.zhixunyun.vo.InspectionTaskSubmitInfo;
import com.zhixunyun.vo.InspectionTaskEquipmentSubmitInfo;
import com.zhixunyun.vo.TaskEquipmentVO;
import com.zhixunyun.vo.TaskInspectionItemVO;
import com.zhixunyun.entity.InspectionTask;
import com.zhixunyun.entity.InspectionTaskEquipment;
import com.zhixunyun.entity.InspectionTaskItem;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface InspectionTaskMapper {


    List<InspectionTaskPageVO> pageQuery(
            InspectionTaskPageQueryDTO dto
    );


    List<InspectionTaskPageVO> pageToday(
            @Param("inspectorId") Long inspectorId,
            @Param("taskDate") LocalDate taskDate,
            @Param("status") InspectionTaskStatus status
    );


    InspectionTaskDetailVO getDetailById(
            Long taskId
    );


    List<TaskEquipmentVO> getTaskEquipment(
            Long taskId
    );


    List<TaskInspectionItemVO> getTaskItems(
            Long taskId
    );


    InspectionTaskStartInfo getStartInfoById(
            Long taskId
    );


    /** 仅 PENDING 可开始；返回 0 表示状态已被并发请求改变。 */
    int startTask(
            @Param("taskId") Long taskId,
            @Param("startedAt") LocalDateTime startedAt
    );


    int cancelTask(
            @Param("taskId") Long taskId,
            @Param("reason") String reason
    );


    InspectionTaskSubmitInfo getSubmitInfoById(
            Long taskId
    );


    /** 提交时用于校验设备集合，并取得记录关联的任务设备行 ID。 */
    List<InspectionTaskEquipmentSubmitInfo> getEquipmentSubmitInfoByTaskId(
            Long taskId
    );

    /** 仅 IN_PROGRESS 可提交；更新 0 行意味着提交权已被其他请求抢先获得。 */
    int completeTask(@Param("taskId") Long taskId,
                     @Param("equipmentCount") int equipmentCount,
                     @Param("completedAt") LocalDateTime completedAt);

    int completeTaskEquipment(@Param("taskId") Long taskId,
                              @Param("completedAt") LocalDateTime completedAt);

    int insertGeneratedTask(InspectionTask task);

    int batchInsertGeneratedEquipment(
            @Param("items") List<InspectionTaskEquipment> items);

    int batchInsertGeneratedItems(
            @Param("items") List<InspectionTaskItem> items);

    List<InspectionTaskEquipment> listEquipmentSnapshots(Long planId);

    List<InspectionTaskItem> listItemSnapshots(Long planId);

    int existsByPlanAndDate(
            @Param("planId") Long planId,
            @Param("taskDate") LocalDate taskDate);

    int markOverdue(@Param("now") LocalDateTime now);

}
