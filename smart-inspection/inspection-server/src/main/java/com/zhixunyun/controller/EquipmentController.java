package com.zhixunyun.controller;

import java.util.List;

import com.zhixunyun.config.Knife4jConfig;
import com.zhixunyun.dto.EquipmentQueryDTO;
import com.zhixunyun.dto.EquipmentSaveDTO;
import com.zhixunyun.dto.EquipmentStatusChangeDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.EquipmentService;
import com.zhixunyun.vo.EquipmentDetailVO;
import com.zhixunyun.vo.EquipmentOptionVO;
import com.zhixunyun.vo.EquipmentVO;
import com.zhixunyun.vo.IdVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "设备管理")
@SecurityRequirement(name = Knife4jConfig.SECURITY_SCHEME_NAME)
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    @Operation(summary = "分页查询设备")
    public Result<PageResult<EquipmentVO>> pageEquipment(
            @Valid EquipmentQueryDTO dto) {
        return Result.success(equipmentService.pageEquipment(dto));
    }

    @PostMapping
    @Operation(summary = "新增设备")
    public Result<IdVO> createEquipment(
            @Valid @RequestBody EquipmentSaveDTO dto) {
        return Result.success(new IdVO(equipmentService.createEquipment(dto)));
    }

    @GetMapping("/options")
    @Operation(summary = "查询正常设备选项")
    public Result<List<EquipmentOptionVO>> listOptions(
            @RequestParam(required = false) Long equipmentTypeId,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) String keyword) {
        return Result.success(
                equipmentService.listOptions(equipmentTypeId, areaId, keyword));
    }

    @GetMapping("/{equipmentId}")
    @Operation(summary = "查询设备详情")
    public Result<EquipmentDetailVO> getEquipment(
            @PathVariable Long equipmentId) {
        return Result.success(equipmentService.getEquipment(equipmentId));
    }

    @PutMapping("/{equipmentId}")
    @Operation(summary = "修改设备档案")
    public Result<Void> updateEquipment(
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentSaveDTO dto) {
        equipmentService.updateEquipment(equipmentId, dto);
        return Result.success();
    }

    @PatchMapping("/{equipmentId}/status")
    @Operation(summary = "启用或停用设备")
    public Result<Void> changeStatus(
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentStatusChangeDTO dto) {
        equipmentService.changeStatus(equipmentId, dto);
        return Result.success();
    }

    @DeleteMapping("/{equipmentId}")
    @Operation(summary = "删除未被业务引用的设备")
    public Result<Void> deleteEquipment(
            @PathVariable Long equipmentId) {
        equipmentService.deleteEquipment(equipmentId);
        return Result.success();
    }
}
