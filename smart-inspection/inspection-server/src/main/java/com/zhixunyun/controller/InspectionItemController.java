package com.zhixunyun.controller;

import java.util.List;

import com.zhixunyun.config.Knife4jConfig;
import com.zhixunyun.dto.InspectionItemQueryDTO;
import com.zhixunyun.dto.InspectionItemSaveDTO;
import com.zhixunyun.dto.InspectionItemStatusDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.InspectionItemService;
import com.zhixunyun.vo.IdVO;
import com.zhixunyun.vo.InspectionItemDetailVO;
import com.zhixunyun.vo.InspectionItemOptionVO;
import com.zhixunyun.vo.InspectionItemVO;
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
@RequestMapping("/api/inspection-items")
@RequiredArgsConstructor
@Tag(name = "巡检项目管理")
@SecurityRequirement(name = Knife4jConfig.SECURITY_SCHEME_NAME)
public class InspectionItemController {

    private final InspectionItemService inspectionItemService;

    @GetMapping
    @Operation(summary = "分页查询巡检项目")
    public Result<PageResult<InspectionItemVO>> pageInspectionItems(
            @Valid InspectionItemQueryDTO dto) {
        return Result.success(inspectionItemService.pageInspectionItems(dto));
    }

    @PostMapping
    @Operation(summary = "新增巡检项目")
    public Result<IdVO> createInspectionItem(
            @Valid @RequestBody InspectionItemSaveDTO dto) {
        return Result.success(new IdVO(
                inspectionItemService.createInspectionItem(dto)));
    }

    @GetMapping("/options")
    @Operation(summary = "按设备类型查询启用巡检项目选项")
    public Result<List<InspectionItemOptionVO>> listOptions(
            @RequestParam Long equipmentTypeId,
            @RequestParam(required = false) String keyword) {
        return Result.success(
                inspectionItemService.listOptions(equipmentTypeId, keyword));
    }

    @GetMapping("/{inspectionItemId}")
    @Operation(summary = "查询巡检项目详情")
    public Result<InspectionItemDetailVO> getInspectionItem(
            @PathVariable Long inspectionItemId) {
        return Result.success(
                inspectionItemService.getInspectionItem(inspectionItemId));
    }

    @PutMapping("/{inspectionItemId}")
    @Operation(summary = "修改巡检项目")
    public Result<Void> updateInspectionItem(
            @PathVariable Long inspectionItemId,
            @Valid @RequestBody InspectionItemSaveDTO dto) {
        inspectionItemService.updateInspectionItem(inspectionItemId, dto);
        return Result.success();
    }

    @PatchMapping("/{inspectionItemId}/status")
    @Operation(summary = "启用或停用巡检项目")
    public Result<Void> changeStatus(
            @PathVariable Long inspectionItemId,
            @Valid @RequestBody InspectionItemStatusDTO dto) {
        inspectionItemService.changeStatus(inspectionItemId, dto);
        return Result.success();
    }
}
