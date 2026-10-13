package com.zhixunyun.service.impl;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.enumeration.EquipmentStatus;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.FaultLevel;
import com.zhixunyun.enumeration.TimeGranularity;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.StatisticsMapper;
import com.zhixunyun.service.StatisticsService;
import com.zhixunyun.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 聚合仪表盘与趋势数据；仪表盘查询按当前角色和用户限定可见范围。 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsMapper mapper;

    @Override
    public DashboardStatisticsVO getDashboard() {
        return mapper.getDashboard(BaseContext.getCurrentRole(), BaseContext.getCurrentId());
    }

    @Override
    public List<WorkOrderTrendPointVO> getWorkOrderTrend(LocalDate startDate, LocalDate endDate,
                                                          TimeGranularity granularity) {
        validateRange(startDate, endDate);
        return mapper.getWorkOrderTrend(startDate, endDate, granularity);
    }

    @Override
    public List<FaultDistributionItemVO> getFaultDistribution(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        Map<FaultLevel, Long> counts = new EnumMap<>(FaultLevel.class);
        mapper.countFaultsByLevel(startDate, endDate)
                .forEach(item -> counts.put(item.getFaultLevel(), item.getCount()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return java.util.Arrays.stream(FaultLevel.values())
                .map(level -> {
                    long count = counts.getOrDefault(level, 0L);
                    return new FaultDistributionItemVO(level, count, ratio(count, total));
                }).toList();
    }

    @Override
    public List<EquipmentStatusDistributionItemVO> getEquipmentStatusDistribution() {
        Map<EquipmentStatus, Long> counts = new EnumMap<>(EquipmentStatus.class);
        mapper.countEquipmentByStatus()
                .forEach(item -> counts.put(item.getStatus(), item.getCount()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return java.util.Arrays.stream(EquipmentStatus.values())
                .map(status -> {
                    long count = counts.getOrDefault(status, 0L);
                    return new EquipmentStatusDistributionItemVO(status, count, ratio(count, total));
                }).toList();
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
    }

    private double ratio(long count, long total) {
        if (total == 0) return 0D;
        return Math.round(count * 10000D / total) / 100D;
    }
}
