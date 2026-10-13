package com.zhixunyun.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.github.pagehelper.Page;
import com.zhixunyun.dto.InspectionPlanPageQueryDTO;
import com.zhixunyun.entity.InspectionPlan;
import com.zhixunyun.vo.EquipmentOptionVO;
import com.zhixunyun.vo.InspectionItemOptionVO;
import com.zhixunyun.vo.InspectionPlanConflictVO;
import com.zhixunyun.vo.InspectionPlanDetailVO;
import com.zhixunyun.vo.InspectionPlanPageVO;
import com.zhixunyun.vo.InspectionPlanScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InspectionPlanMapper {

    int insert(InspectionPlan plan);

    int batchInsertEquipment(
            @Param("planId") Long planId,
            @Param("equipmentIds") List<Long> equipmentIds
    );

    int batchInsertItems(
            @Param("planId") Long planId,
            @Param("itemIds") List<Long> itemIds
    );

    int batchInsertSchedule(
            @Param("planId") Long planId,
            @Param("dayValues") List<Integer> dayValues
    );
    /**
     * 仅筛出同设备、同执行时刻且日期区间相交的计划；
     * 周期是否共享实际执行日由服务层继续判断。
     */
    List<InspectionPlanConflictVO> findConflictCandidates(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("executionTime") LocalTime executionTime,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludePlanId") Long excludePlanId
    );
    List<InspectionPlanScheduleVO> findSchedulesByPlanIds(
            @Param("planIds") List<Long> planIds
    );

    /**
     * 根据名称查询计划
     */
    @Select("""
    select *
    from inspection_plan
    where name = #{name}
    limit 1
    """)
    InspectionPlan getByName(String name);

    /**
     * 分页查询计划
     */
    Page<InspectionPlanPageVO> pageQuery(InspectionPlanPageQueryDTO dto);

    /** 管理操作和任务生成共用计划行锁，串行化配置与生成游标变化。 */
    InspectionPlan getByIdForUpdate(Long inspectionPlanId);

    InspectionPlanDetailVO getDetailById(Long inspectionPlanId);

    List<EquipmentOptionVO> listEquipmentByPlanId(Long inspectionPlanId);

    List<InspectionItemOptionVO> listItemsByPlanId(Long inspectionPlanId);

    List<Long> listEquipmentIdsByPlanId(Long inspectionPlanId);

    List<Long> listItemIdsByPlanId(Long inspectionPlanId);

    List<Integer> listScheduleDaysByPlanId(Long inspectionPlanId);

    int update(InspectionPlan plan);

    int updateStatus(
            @Param("inspectionPlanId") Long inspectionPlanId,
            @Param("enabled") boolean enabled,
            @Param("nextGenerateAt") LocalDateTime nextGenerateAt);

    int deleteEquipmentRelations(Long inspectionPlanId);

    int deleteItemRelations(Long inspectionPlanId);

    int deleteScheduleRelations(Long inspectionPlanId);

    int countTasks(Long inspectionPlanId);

    int deleteById(Long inspectionPlanId);

    List<Long> listDuePlanIds(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    /** 只在启用且游标仍等于 expected 时推进，返回 0 表示条件已变化。 */
    int updateNextGenerateAt(
            @Param("planId") Long planId,
            @Param("expected") LocalDateTime expected,
            @Param("next") LocalDateTime next);
}
