package com.zhixunyun.mapper;

import com.zhixunyun.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper {
    int insert(SysOperationLog operationLog);
}
