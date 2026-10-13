package com.zhixunyun.controller;

import java.util.List;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.dto.WorkOrderAssignRequest;
import com.zhixunyun.dto.WorkOrderQueryDTO;
import com.zhixunyun.dto.WorkOrderResultSubmitRequest;
import com.zhixunyun.dto.WorkOrderVerifyRequest;
import com.zhixunyun.dto.RemarkRequest;
import com.zhixunyun.dto.ReasonDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.WorkOrderService;
import com.zhixunyun.vo.WorkOrderDetailVO;
import com.zhixunyun.vo.WorkOrderOperationVO;
import com.zhixunyun.vo.WorkOrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 维修工单接口区分管理员派单验收与维修员接单处理。
 * 维修员可调用接口后，服务层还会核对工单是否实际指派给本人。
 */
@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Tag(name = "11. 维修工单")
public class WorkOrderController {
    private final WorkOrderService service;

    @GetMapping
    @RequireRole({"ADMIN", "MAINTAINER"})
    public Result<PageResult<WorkOrderVO>> page(@Valid WorkOrderQueryDTO dto) {
        return Result.success(service.pageQuery(dto));
    }

    @GetMapping("/{workOrderId}")
    @RequireRole({"ADMIN", "MAINTAINER"})
    public Result<WorkOrderDetailVO> detail(@PathVariable Long workOrderId) {
        return Result.success(service.getById(workOrderId));
    }

    @GetMapping("/{workOrderId}/operations")
    @RequireRole({"ADMIN", "MAINTAINER"})
    public Result<List<WorkOrderOperationVO>> operations(@PathVariable Long workOrderId) {
        return Result.success(service.listOperations(workOrderId));
    }
    @PostMapping("/{workOrderId}/assign")
    @RequireRole("ADMIN")
    public Result<Void> assign(
            @PathVariable Long workOrderId,
            @RequestBody @Valid WorkOrderAssignRequest request) {

        service.assign(workOrderId, request);

        return Result.success();
    }

    @PostMapping("/{workOrderId}/accept")
    @RequireRole("MAINTAINER")
    public Result<Void> accept(@PathVariable Long workOrderId) {
        service.accept(workOrderId);
        return Result.success();
    }

    @PostMapping("/{workOrderId}/submit-result")
    @RequireRole("MAINTAINER")
    public Result<Void> submitResult(
            @PathVariable Long workOrderId,
            @RequestBody @Valid WorkOrderResultSubmitRequest request) {
        service.submitResult(workOrderId, request);
        return Result.success();
    }

    @PostMapping("/{workOrderId}/verify")
    @RequireRole("ADMIN")
    public Result<Void> verify(
            @PathVariable Long workOrderId,
            @RequestBody @Valid WorkOrderVerifyRequest request) {
        service.verify(workOrderId, request);
        return Result.success();
    }

    @PostMapping("/{workOrderId}/rework")
    @RequireRole("MAINTAINER")
    public Result<Void> rework(
            @PathVariable Long workOrderId,
            @RequestBody(required = false) @Valid RemarkRequest request) {
        service.rework(workOrderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{workOrderId}/cancel")
    @RequireRole("ADMIN")
    public Result<Void> cancel(
            @PathVariable Long workOrderId,
            @RequestBody @Valid ReasonDTO request) {
        service.cancel(workOrderId, request.getReason());
        return Result.success();
    }
}
