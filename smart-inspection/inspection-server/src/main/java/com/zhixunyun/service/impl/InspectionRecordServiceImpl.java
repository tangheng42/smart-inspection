package com.zhixunyun.service.impl;

import java.util.List;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.PageHelper;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.dto.InspectionRecordQueryDTO;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.RoleCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.InspectionRecordMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.InspectionRecordService;
import com.zhixunyun.vo.InspectionRecordDetailVO;
import com.zhixunyun.vo.InspectionRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 查询巡检结果；列表及详情分别按当前巡检员限制数据范围。 */
@Service
@RequiredArgsConstructor
public class InspectionRecordServiceImpl implements InspectionRecordService {
    private final InspectionRecordMapper mapper;

    @Override
    public PageResult<InspectionRecordVO> pageQuery(InspectionRecordQueryDTO dto) {
        validateRange(dto);
        dto.setTaskNo(trimToNull(dto.getTaskNo()));
        if (RoleCode.INSPECTOR.name().equals(BaseContext.getCurrentRole())) {
            dto.setInspectorId(BaseContext.getCurrentId());
        }
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<InspectionRecordVO> records = mapper.pageQuery(dto);
        return new PageResult<>(dto.getPageNum(), dto.getPageSize(),
                new PageInfo<>(records).getTotal(), records);
    }

    @Override
    public InspectionRecordDetailVO getById(Long id) {
        InspectionRecordDetailVO detail = mapper.getDetailById(id);
        if (detail == null) throw new BusinessException(ErrorCode.INSPECTION_RECORD_NOT_FOUND);
        if (RoleCode.INSPECTOR.name().equals(BaseContext.getCurrentRole())
                && !BaseContext.getCurrentId().equals(detail.getInspectorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        detail.setItemResults(mapper.listItemResults(id));
        detail.setImageUrls(mapper.listImageUrls(id));
        return detail;
    }

    private void validateRange(InspectionRecordQueryDTO dto) {
        if (dto.getCompletedFrom() != null && dto.getCompletedTo() != null
                && dto.getCompletedFrom().isAfter(dto.getCompletedTo())) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
