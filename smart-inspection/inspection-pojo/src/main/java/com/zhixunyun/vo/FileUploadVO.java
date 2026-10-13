package com.zhixunyun.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadVO {
    private String objectKey;
    private String url;
    private String originalName;
    private String contentType;
    private Long size;
}
