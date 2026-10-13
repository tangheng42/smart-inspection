package com.zhixunyun.mapper;

import java.time.LocalDateTime;
import java.util.List;
import com.zhixunyun.dto.FaultRecordQueryDTO;
import com.zhixunyun.entity.FaultRecord;
import com.zhixunyun.vo.FaultRecordDetailVO;
import com.zhixunyun.vo.FaultRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FaultRecordMapper {
    List<FaultRecordVO> pageQuery(FaultRecordQueryDTO dto);
    FaultRecordDetailVO getDetailById(Long faultRecordId);
    boolean maintainerCanAccess(@Param("faultRecordId") Long faultRecordId,
                                @Param("maintainerId") Long maintainerId);
    List<String> listImageUrls(Long faultRecordId);
    int insert(FaultRecord faultRecord);
    int markProcessing(@Param("faultRecordId") Long faultRecordId);
    int close(@Param("faultRecordId") Long faultRecordId,
              @Param("closedAt") LocalDateTime closedAt);
}
