package com.zhixunyun.controller;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.enumeration.FileBizType;
import com.zhixunyun.result.Result;
import com.zhixunyun.service.FileService;
import com.zhixunyun.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireRole({"ADMIN", "INSPECTOR", "MAINTAINER"})
    public Result<FileUploadVO> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam FileBizType bizType) {
        return Result.success(fileService.uploadImage(file, bizType));
    }
}
