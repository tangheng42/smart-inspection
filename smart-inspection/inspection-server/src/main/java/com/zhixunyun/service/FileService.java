package com.zhixunyun.service;

import com.zhixunyun.enumeration.FileBizType;
import com.zhixunyun.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadVO uploadImage(MultipartFile file, FileBizType bizType);
}
