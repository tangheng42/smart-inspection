package com.zhixunyun.controller;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.dto.InspectionTaskPageQueryDTO;
import com.zhixunyun.dto.InspectionTaskSubmitRequest;
import com.zhixunyun.dto.InspectionTaskTodayQueryDTO;
import com.zhixunyun.dto.ReasonDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.InspectionTaskService;
import com.zhixunyun.vo.InspectionTaskDetailVO;
import com.zhixunyun.vo.InspectionTaskPageVO;
import com.zhixunyun.vo.InspectionSubmitDataVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 巡检任务接口的角色边界：管理员可取消待执行任务，巡检员可开始并提交自己的任务。
 * 任务归属和状态约束由服务层检查。
 */
@RestController
@RequestMapping("/api/inspection-tasks")
@RequiredArgsConstructor
public class InspectionTaskController {

    private final InspectionTaskService inspectionTaskService;

    /**
     * 分页查询巡检任务
     */
    @GetMapping
    @RequireRole({"ADMIN", "INSPECTOR"})
    public Result<PageResult<InspectionTaskPageVO>> page(
            @Valid InspectionTaskPageQueryDTO dto) {

        PageResult<InspectionTaskPageVO> pageResult =
                inspectionTaskService.pageQuery(dto);

        return Result.success(pageResult);
    }

    /**
     * 分页查询今日巡检任务
     */
    @GetMapping("/today")
    @RequireRole("INSPECTOR")
    public Result<PageResult<InspectionTaskPageVO>> today(
            @Valid InspectionTaskTodayQueryDTO dto) {

        return Result.success(
                inspectionTaskService.pageToday(dto)
        );
    }

    /**
     * 查询巡检任务详情
     */
    @GetMapping("/{inspectionTaskId}")
    @RequireRole({"ADMIN", "INSPECTOR"})
    public Result<InspectionTaskDetailVO> getById(
            @PathVariable Long inspectionTaskId) {

        return Result.success(
                inspectionTaskService.getById(inspectionTaskId)
        );
    }

    /**
     * 启动巡检任务
     */
    @PostMapping("/{inspectionTaskId}/start")
    @RequireRole("INSPECTOR")
    public Result<Void> start(
            @PathVariable Long inspectionTaskId) {

        inspectionTaskService.start(inspectionTaskId);

        return Result.success();
    }

    /**
     *提交巡检结果
     */
    @PostMapping("/{inspectionTaskId}/submit")
    @RequireRole("INSPECTOR")
    public Result<InspectionSubmitDataVO> submit(
            @PathVariable Long inspectionTaskId,
            @RequestBody @Valid InspectionTaskSubmitRequest request
    ) {

        return Result.success(inspectionTaskService.submit(inspectionTaskId, request));
    }

    /**
     *取消巡检任务
     */
    @PostMapping("/{inspectionTaskId}/cancel")
    @RequireRole("ADMIN")
    public Result<Void> cancel(@PathVariable Long inspectionTaskId,
                               @Valid @RequestBody ReasonDTO dto) {
        inspectionTaskService.cancel(inspectionTaskId, dto.getReason());
        return Result.success();
    }
}
