package com.zhixunyun.controller;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.dto.FaultRecordQueryDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.FaultRecordService;
import com.zhixunyun.vo.FaultRecordDetailVO;
import com.zhixunyun.vo.FaultRecordVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fault-records")
@RequiredArgsConstructor
@Tag(name = "10. 异常记录")
public class FaultRecordController {
    private final FaultRecordService service;

    @GetMapping
    @RequireRole({"ADMIN", "INSPECTOR", "MAINTAINER"})
    public Result<PageResult<FaultRecordVO>> page(@Valid FaultRecordQueryDTO dto) {
        return Result.success(service.pageQuery(dto));
    }

    @GetMapping("/{faultRecordId}")
    @RequireRole({"ADMIN", "INSPECTOR", "MAINTAINER"})
    public Result<FaultRecordDetailVO> detail(@PathVariable Long faultRecordId) {
        return Result.success(service.getById(faultRecordId));
    }
}
