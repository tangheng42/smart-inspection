package com.zhixunyun.controller;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.config.Knife4jConfig;
import com.zhixunyun.dto.InspectionPlanPageQueryDTO;
import com.zhixunyun.dto.InspectionPlanSaveDTO;
import com.zhixunyun.dto.InspectionPlanStatusDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.InspectionPlanService;
import com.zhixunyun.vo.IdVO;
import com.zhixunyun.vo.InspectionPlanDetailVO;
import com.zhixunyun.vo.InspectionPlanPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员维护巡检计划；计划配置和时间冲突的完整校验位于服务层。 */
@RestController
@RequestMapping("/api/inspection-plans")
@RequiredArgsConstructor
@Tag(name = "07. 巡检计划管理")
@SecurityRequirement(name = Knife4jConfig.SECURITY_SCHEME_NAME)
public class InspectionPlanController {

    private final InspectionPlanService inspectionPlanService;

    /**
     * 创建巡检计划
     */
    @PostMapping
    @RequireRole("ADMIN")
    @Operation(
            summary = "创建巡检计划",
            description = "创建计划主表，并保存设备、巡检项目和周期明细；同一设备在相同执行时刻不能发生计划冲突。"
    )
    public Result<IdVO> create(
            @Valid @RequestBody InspectionPlanSaveDTO dto) {

        Long planId = inspectionPlanService.create(dto);

        return Result.success(new IdVO(planId));
    }

    /**
     * 分页查询巡检计划
     */
    @GetMapping
    @RequireRole("ADMIN")
    @Operation(summary = "分页查询巡检计划")
    public Result<PageResult<InspectionPlanPageVO>> page(
            @Valid InspectionPlanPageQueryDTO dto) {

        PageResult<InspectionPlanPageVO> pageResult =
                inspectionPlanService.pageQuery(dto);

        return Result.success(pageResult);
    }

    @GetMapping("/{inspectionPlanId}")
    @RequireRole("ADMIN")
    @Operation(summary = "查询巡检计划详情")
    public Result<InspectionPlanDetailVO> getById(
            @PathVariable Long inspectionPlanId) {
        return Result.success(
                inspectionPlanService.getById(inspectionPlanId));
    }

    @PutMapping("/{inspectionPlanId}")
    @RequireRole("ADMIN")
    @Operation(
            summary = "修改巡检计划",
            description = "在同一事务中修改计划主表并替换设备、巡检项目和周期明细；只影响以后生成的任务。")
    public Result<Void> update(
            @PathVariable Long inspectionPlanId,
            @Valid @RequestBody InspectionPlanSaveDTO dto) {
        inspectionPlanService.update(inspectionPlanId, dto);
        return Result.success();
    }

    @PatchMapping("/{inspectionPlanId}/status")
    @RequireRole("ADMIN")
    @Operation(
            summary = "启用或停用巡检计划",
            description = "停用后不再生成新任务；重新启用时重新校验关联数据和时间冲突。")
    public Result<Void> changeStatus(
            @PathVariable Long inspectionPlanId,
            @Valid @RequestBody InspectionPlanStatusDTO dto) {
        inspectionPlanService.changeStatus(inspectionPlanId, dto);
        return Result.success();
    }

    @DeleteMapping("/{inspectionPlanId}")
    @RequireRole("ADMIN")
    @Operation(
            summary = "删除从未生成任务的巡检计划",
            description = "只允许删除没有生成过巡检任务的计划；有关联任务时应改为停用。")
    public Result<Void> delete(
            @PathVariable Long inspectionPlanId) {
        inspectionPlanService.delete(inspectionPlanId);
        return Result.success();
    }
}
