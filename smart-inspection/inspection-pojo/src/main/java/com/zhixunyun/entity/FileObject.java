package com.zhixunyun.entity;

import lombok.Data;

@Data
public class FileObject {
    private Long id;
    private String bucketName;
    private String objectKey;
    private String url;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private Long uploaderId;
}
