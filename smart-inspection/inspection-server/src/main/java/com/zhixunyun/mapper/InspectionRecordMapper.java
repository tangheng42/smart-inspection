package com.zhixunyun.mapper;

import java.util.List;

import com.zhixunyun.dto.InspectionRecordQueryDTO;
import com.zhixunyun.entity.InspectionRecord;
import com.zhixunyun.entity.InspectionRecordItem;
import com.zhixunyun.vo.InspectionItemResultVO;
import com.zhixunyun.vo.InspectionRecordDetailVO;
import com.zhixunyun.vo.InspectionRecordVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InspectionRecordMapper {
    List<InspectionRecordVO> pageQuery(InspectionRecordQueryDTO dto);
    InspectionRecordDetailVO getDetailById(Long inspectionRecordId);
    List<InspectionItemResultVO> listItemResults(Long inspectionRecordId);
    List<String> listImageUrls(Long inspectionRecordId);
    /**
     * 新增巡检记录
     */
    int insert(InspectionRecord record);

    int insertItem(InspectionRecordItem item);
}
