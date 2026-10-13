package com.zhixunyun.service;

import java.time.LocalDate;
import java.util.List;
import com.zhixunyun.enumeration.TimeGranularity;
import com.zhixunyun.vo.EquipmentStatusDistributionItemVO;
import com.zhixunyun.vo.FaultDistributionItemVO;
import com.zhixunyun.vo.WorkOrderTrendPointVO;
import com.zhixunyun.vo.DashboardStatisticsVO;

public interface StatisticsService {
    DashboardStatisticsVO getDashboard();
    List<WorkOrderTrendPointVO> getWorkOrderTrend(LocalDate startDate, LocalDate endDate,
                                                   TimeGranularity granularity);
    List<FaultDistributionItemVO> getFaultDistribution(LocalDate startDate, LocalDate endDate);
    List<EquipmentStatusDistributionItemVO> getEquipmentStatusDistribution();
}
