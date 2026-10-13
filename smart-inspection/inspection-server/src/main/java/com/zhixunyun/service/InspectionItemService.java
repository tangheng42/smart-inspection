package com.zhixunyun.service;

import java.util.List;

import com.zhixunyun.dto.InspectionItemQueryDTO;
import com.zhixunyun.dto.InspectionItemSaveDTO;
import com.zhixunyun.dto.InspectionItemStatusDTO;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.vo.InspectionItemDetailVO;
import com.zhixunyun.vo.InspectionItemOptionVO;
import com.zhixunyun.vo.InspectionItemVO;

public interface InspectionItemService {

    PageResult<InspectionItemVO> pageInspectionItems(InspectionItemQueryDTO dto);

    Long createInspectionItem(InspectionItemSaveDTO dto);

    List<InspectionItemOptionVO> listOptions(Long equipmentTypeId, String keyword);

    InspectionItemDetailVO getInspectionItem(Long inspectionItemId);

    void updateInspectionItem(Long inspectionItemId, InspectionItemSaveDTO dto);

    void changeStatus(Long inspectionItemId, InspectionItemStatusDTO dto);
}
