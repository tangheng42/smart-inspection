package com.zhixunyun.mapper;

import java.time.LocalDate;
import java.util.List;
import com.zhixunyun.enumeration.TimeGranularity;
import com.zhixunyun.vo.CountByEquipmentStatusVO;
import com.zhixunyun.vo.CountByFaultLevelVO;
import com.zhixunyun.vo.DashboardStatisticsVO;
import com.zhixunyun.vo.WorkOrderTrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatisticsMapper {
    List<WorkOrderTrendPointVO> getWorkOrderTrend(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("granularity") TimeGranularity granularity);
    List<CountByFaultLevelVO> countFaultsByLevel(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
    List<CountByEquipmentStatusVO> countEquipmentByStatus();
    DashboardStatisticsVO getDashboard(@Param("role") String role,
                                       @Param("userId") Long userId);
}
