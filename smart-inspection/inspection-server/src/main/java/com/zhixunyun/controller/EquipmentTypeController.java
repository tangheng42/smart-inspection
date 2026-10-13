package com.zhixunyun.controller;

import java.util.List;

import com.zhixunyun.dto.EquipmentTypeQueryDTO;
import com.zhixunyun.dto.EquipmentTypeSaveDTO;
import com.zhixunyun.dto.EquipmentTypeStatusDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.EquipmentTypeService;
import com.zhixunyun.vo.EquipmentTypeDetailVO;
import com.zhixunyun.vo.EquipmentTypeOptionVO;
import com.zhixunyun.vo.EquipmentTypeVO;
import com.zhixunyun.vo.IdVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/equipment-types")
@RequiredArgsConstructor
@Tag(name = "设备类型管理")
@SecurityRequirement(name = "BearerAuth")
public class EquipmentTypeController {

    private final EquipmentTypeService equipmentTypeService;

    @GetMapping
    @Operation(summary = "分页查询设备类型")
    public Result<PageResult<EquipmentTypeVO>> pageQuery(
            @Valid EquipmentTypeQueryDTO dto) {

        return Result.success(equipmentTypeService.pageQuery(dto));
    }

    @PostMapping
    @Operation(summary = "新增设备类型")
    public Result<IdVO> createEquipmentType(
            @Valid @RequestBody EquipmentTypeSaveDTO dto) {

        Long equipmentTypeId = equipmentTypeService.createEquipmentType(dto);
        return Result.success(new IdVO(equipmentTypeId));
    }

    @GetMapping("/options")
    @Operation(summary = "查询启用设备类型选项")
    public Result<List<EquipmentTypeOptionVO>> listOptions(
            @RequestParam(required = false) String keyword) {

        return Result.success(equipmentTypeService.listOptions(keyword));
    }

    @GetMapping("/{equipmentTypeId}")
    @Operation(summary = "查询设备类型详情")
    public Result<EquipmentTypeDetailVO> getEquipmentType(
            @PathVariable Long equipmentTypeId) {

        return Result.success(equipmentTypeService.getEquipmentType(equipmentTypeId));
    }

    @PutMapping("/{equipmentTypeId}")
    @Operation(summary = "修改设备类型")
    public Result<Void> updateEquipmentType(
            @PathVariable Long equipmentTypeId,
            @Valid @RequestBody EquipmentTypeSaveDTO dto) {

        equipmentTypeService.updateEquipmentType(equipmentTypeId, dto);
        return Result.success();
    }

    @PatchMapping("/{equipmentTypeId}/status")
    @Operation(summary = "启用或停用设备类型")
    public Result<Void> changeStatus(
            @PathVariable Long equipmentTypeId,
            @Valid @RequestBody EquipmentTypeStatusDTO dto) {

        equipmentTypeService.changeStatus(equipmentTypeId, dto);
        return Result.success();
    }
}
