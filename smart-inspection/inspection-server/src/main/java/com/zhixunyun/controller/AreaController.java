package com.zhixunyun.controller;

import java.util.List;

import com.zhixunyun.dto.AreaCreateDTO;
import com.zhixunyun.dto.AreaQueryDTO;
import com.zhixunyun.dto.AreaStatusDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.AreaService;
import com.zhixunyun.vo.AreaDetailVO;
import com.zhixunyun.vo.AreaOptionVO;
import com.zhixunyun.vo.AreaVO;
import com.zhixunyun.vo.IdVO;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/areas")
@RequiredArgsConstructor
@Tag(name = "区域管理")
public class AreaController {

    private final AreaService areaService;

    /**
     * 分页查询区域
     */
    @GetMapping
    @Operation(summary = "分页查询区域")
    public Result<PageResult<AreaVO>> pageAreas(
            @Valid AreaQueryDTO dto) {

        return Result.success(areaService.pageAreas(dto));
    }

    /**
     * 新增区域
     */
    @PostMapping
    @Operation(summary = "新增区域")
    public Result<IdVO> addArea(
            @Valid @RequestBody AreaCreateDTO dto) {

        Long areaId = areaService.createArea(dto);

        IdVO idVO = new IdVO(areaId);

        return Result.success(idVO);
    }

    /**
     * 查询启用区域选项
     */
    @GetMapping("/options")
    @Operation(summary = "查询启用区域选项")
    public Result<List<AreaOptionVO>> listAreaOptions(
            @RequestParam(required = false) String keyword) {

        return Result.success(areaService.listAreaOptions(keyword));
    }


    /**
     * 查询区域详情
     */
    @GetMapping("/{areaId}")
    @Operation(summary = "查询区域详情")
    public Result<AreaDetailVO> getArea(
            @PathVariable("areaId") Long areaId) {

        return Result.success(areaService.getArea(areaId));
    }

    /**
     * 修改区域
     */
    @PutMapping("/{areaId}")
    @Operation(summary = "修改区域")
    public Result<Void> updateArea(
            @PathVariable("areaId") Long areaId,
            @Valid @RequestBody AreaCreateDTO dto) {

        areaService.updateArea(areaId, dto);

        return Result.success();
    }

    /**
     * 启用或停用区域
     */
    @PatchMapping("/{areaId}/status")
    @Operation(summary = "启用或停用区域")
    public Result<Void> changeAreaStatus(
            @PathVariable("areaId") Long areaId,
            @Valid @RequestBody AreaStatusDTO dto) {

        areaService.changeAreaStatus(areaId, dto);

        return Result.success();
    }
}
