package com.zhixunyun.mapper;

import java.util.List;
import com.zhixunyun.entity.BizAttachment;
import com.zhixunyun.entity.FileObject;
import com.zhixunyun.vo.FileReferenceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttachmentMapper {
    List<FileReferenceVO> findFilesByUrls(
            @Param("urls") List<String> urls,
            @Param("uploaderId") Long uploaderId);
    int insert(BizAttachment attachment);
    int insertFile(FileObject fileObject);
}
