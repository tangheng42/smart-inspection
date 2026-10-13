package com.zhixunyun.service;

import com.zhixunyun.dto.FaultRecordQueryDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.FaultRecordDetailVO;
import com.zhixunyun.vo.FaultRecordVO;

public interface FaultRecordService {
    PageResult<FaultRecordVO> pageQuery(FaultRecordQueryDTO dto);
    FaultRecordDetailVO getById(Long faultRecordId);
}
