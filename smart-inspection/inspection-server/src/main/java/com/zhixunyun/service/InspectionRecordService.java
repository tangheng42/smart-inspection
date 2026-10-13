package com.zhixunyun.service;

import com.zhixunyun.dto.InspectionRecordQueryDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.InspectionRecordDetailVO;
import com.zhixunyun.vo.InspectionRecordVO;

public interface InspectionRecordService {
    PageResult<InspectionRecordVO> pageQuery(InspectionRecordQueryDTO dto);
    InspectionRecordDetailVO getById(Long inspectionRecordId);
}
