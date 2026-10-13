package com.zhixunyun.controller;

import java.time.LocalDate;
import java.util.List;
import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.enumeration.TimeGranularity;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.StatisticsService;
import com.zhixunyun.vo.EquipmentStatusDistributionItemVO;
import com.zhixunyun.vo.FaultDistributionItemVO;
import com.zhixunyun.vo.WorkOrderTrendPointVO;
import com.zhixunyun.vo.DashboardStatisticsVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "14. 数据统计")
public class StatisticsController {
    private final StatisticsService service;

    @GetMapping("/dashboard")
    @RequireRole({"ADMIN", "INSPECTOR", "MAINTAINER"})
    public Result<DashboardStatisticsVO> dashboard() {
        return Result.success(service.getDashboard());
    }

    @GetMapping("/work-orders/trend")
    @RequireRole("ADMIN")
    public Result<List<WorkOrderTrendPointVO>> workOrderTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") TimeGranularity granularity) {
        return Result.success(service.getWorkOrderTrend(startDate, endDate, granularity));
    }

    @GetMapping("/faults/distribution")
    @RequireRole("ADMIN")
    public Result<List<FaultDistributionItemVO>> faultDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(service.getFaultDistribution(startDate, endDate));
    }

    @GetMapping("/equipment/status-distribution")
    @RequireRole("ADMIN")
    public Result<List<EquipmentStatusDistributionItemVO>> equipmentStatusDistribution() {
        return Result.success(service.getEquipmentStatusDistribution());
    }
}
