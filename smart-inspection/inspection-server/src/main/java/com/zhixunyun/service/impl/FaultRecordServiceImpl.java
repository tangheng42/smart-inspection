package com.zhixunyun.service.impl;

import java.util.List;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.FaultRecordQueryDTO;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.FaultRecordMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.FaultRecordService;
import com.zhixunyun.vo.FaultRecordDetailVO;
import com.zhixunyun.vo.FaultRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 查询巡检异常；巡检员只能查看本人上报，维修员只能查看关联工单中的异常。 */
@Service
@RequiredArgsConstructor
public class FaultRecordServiceImpl implements FaultRecordService {
    private final FaultRecordMapper mapper;

    @Override
    public PageResult<FaultRecordVO> pageQuery(FaultRecordQueryDTO dto) {
        validateRange(dto);
        dto.setFaultNo(trimToNull(dto.getFaultNo()));
        String role = BaseContext.getCurrentRole();
        if (RoleCode.INSPECTOR.name().equals(role)) {
            dto.setReporterId(BaseContext.getCurrentId());
            dto.setMaintainerId(null);
        } else if (RoleCode.MAINTAINER.name().equals(role)) {
            dto.setMaintainerId(BaseContext.getCurrentId());
            dto.setReporterId(null);
        }
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<FaultRecordVO> records = mapper.pageQuery(dto);
        return new PageResult<>(dto.getPageNum(), dto.getPageSize(),
                new PageInfo<>(records).getTotal(), records);
    }

    @Override
    public FaultRecordDetailVO getById(Long id) {
        FaultRecordDetailVO detail = mapper.getDetailById(id);
        if (detail == null) throw new BusinessException(ErrorCode.FAULT_RECORD_NOT_FOUND);
        String role = BaseContext.getCurrentRole();
        Long userId = BaseContext.getCurrentId();
        if (RoleCode.INSPECTOR.name().equals(role) && !userId.equals(detail.getReporterId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (RoleCode.MAINTAINER.name().equals(role) && !mapper.maintainerCanAccess(id, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        detail.setImageUrls(mapper.listImageUrls(id));
        return detail;
    }

    private void validateRange(FaultRecordQueryDTO dto) {
        if (dto.getReportedFrom() != null && dto.getReportedTo() != null
                && dto.getReportedFrom().isAfter(dto.getReportedTo())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
