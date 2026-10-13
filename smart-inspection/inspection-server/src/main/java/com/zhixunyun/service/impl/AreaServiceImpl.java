package com.zhixunyun.service.impl;

import com.zhixunyun.annotation.OperationLog;

import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhixunyun.dto.AreaCreateDTO;
import com.zhixunyun.dto.AreaQueryDTO;
import com.zhixunyun.dto.AreaStatusDTO;
import com.zhixunyun.entity.Area;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.AreaMapper;
import com.zhixunyun.result.PageResult;
import com.zhixunyun.service.AreaService;
import com.zhixunyun.service.OptionCacheService;
import com.zhixunyun.vo.AreaDetailVO;
import com.zhixunyun.vo.AreaOptionVO;
import com.zhixunyun.vo.AreaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 区域档案维护及下拉选项缓存；停用前需确认没有关联设备。 */
@Service
@RequiredArgsConstructor
public class AreaServiceImpl implements AreaService {

    private final AreaMapper areaMapper;
    private final OptionCacheService optionCacheService;

    /**
     * 分页查询区域
     */
    @Override
    public PageResult<AreaVO> pageAreas(AreaQueryDTO dto) {

        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());

        List<AreaVO> list = areaMapper.pageQuery(dto);

        PageInfo<AreaVO> pageInfo = new PageInfo<>(list);

        return new PageResult<>(
                dto.getPageNum(),
                dto.getPageSize(),
                pageInfo.getTotal(),
                list
        );
    }

    /**
     * 新增区域
     */
    @Override
    @OperationLog(value = "新增区域", businessType = "AREA")
    public Long createArea(AreaCreateDTO dto) {

        Area existArea = areaMapper.getByCode(dto.getCode());

        if (existArea != null) {
            throw new BusinessException(ErrorCode.AREA_EXIST_CODE);
        }

        Area area = new Area();
        area.setCode(dto.getCode());
        area.setName(dto.getName());
        area.setDescription(dto.getDescription());
        area.setEnabled(1);

        areaMapper.insert(area);

        optionCacheService.evict(OptionCacheService.AREA_OPTIONS_KEY);

        return area.getId();
    }

    /** 无关键字时读取并回填缓存；搜索请求直接查询数据库，避免无限增长的缓存键。 */
    @Override
    public List<AreaOptionVO> listAreaOptions(String keyword) {

        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }
        if (normalizedKeyword == null) {
            List<AreaOptionVO> cached = optionCacheService.getList(
                    OptionCacheService.AREA_OPTIONS_KEY, AreaOptionVO.class);
            if (cached != null) {
                return cached;
            }
        }

        List<Area> list = areaMapper.getAreaOptions(normalizedKeyword);

        List<AreaOptionVO> result = list.stream()
                .map(area -> {
                    AreaOptionVO vo = new AreaOptionVO();
                    vo.setId(area.getId());
                    vo.setCode(area.getCode());
                    vo.setName(area.getName());
                    return vo;
                })
                .toList();
        if (normalizedKeyword == null) {
            optionCacheService.put(OptionCacheService.AREA_OPTIONS_KEY, result);
        }
        return result;
    }

    /**
     * 查询区域详情
     */
    @Override
    public AreaDetailVO getArea(Long areaId) {

        AreaDetailVO vo = areaMapper.getDetailById(areaId);

        if (vo == null) {
            throw new BusinessException(ErrorCode.AREA_NOT_FOUND);
        }

        return vo;
    }

    @Override
    @OperationLog(value = "修改区域", businessType = "AREA")
    public void updateArea(Long areaId, AreaCreateDTO dto) {

        Area area = areaMapper.getById(areaId);

        if (area == null) {
            throw new BusinessException(ErrorCode.AREA_NOT_FOUND);
        }

        area.setCode(dto.getCode());
        area.setName(dto.getName());
        area.setDescription(dto.getDescription());

        areaMapper.update(area);
        optionCacheService.evict(OptionCacheService.AREA_OPTIONS_KEY);
    }

    /**
     * 启用或停用区域
     */
    @Override
    @OperationLog(value = "启停区域", businessType = "AREA")
    public void changeAreaStatus(Long areaId, AreaStatusDTO dto) {
        Area area = areaMapper.getById(areaId);
        if (area == null) {
            throw new BusinessException(ErrorCode.AREA_NOT_FOUND);
        }
        // 已有设备仍引用该区域时不允许停用，避免设备失去可用的区域归属。
        if (Boolean.FALSE.equals(dto.getEnabled())) {

            int count = areaMapper.countEquipmentByAreaId(areaId);

            if (count > 0) {
                throw new BusinessException(ErrorCode.AREA_HAS_EQUIPMENT);
            }
        }
        area.setEnabled(dto.getEnabled() ? 1 : 0);
        areaMapper.update(area);
        optionCacheService.evict(OptionCacheService.AREA_OPTIONS_KEY);
    }
}
