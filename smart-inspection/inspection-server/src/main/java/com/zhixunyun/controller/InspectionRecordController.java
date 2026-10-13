package com.zhixunyun.controller;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.dto.InspectionRecordQueryDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.InspectionRecordService;
import com.zhixunyun.vo.InspectionRecordDetailVO;
import com.zhixunyun.vo.InspectionRecordVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inspection-records")
@RequiredArgsConstructor
@Tag(name = "09. 巡检记录")
public class InspectionRecordController {
    private final InspectionRecordService service;

    @GetMapping
    @RequireRole({"ADMIN", "INSPECTOR"})
    public Result<PageResult<InspectionRecordVO>> page(@Valid InspectionRecordQueryDTO dto) {
        return Result.success(service.pageQuery(dto));
    }

    @GetMapping("/{inspectionRecordId}")
    @RequireRole({"ADMIN", "INSPECTOR"})
    public Result<InspectionRecordDetailVO> detail(@PathVariable Long inspectionRecordId) {
        return Result.success(service.getById(inspectionRecordId));
    }
}
