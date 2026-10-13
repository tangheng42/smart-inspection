package com.zhixunyun.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zhixunyun.minio")
public class MinioProperties {
    private String endpoint;
    private String publicUrl;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private boolean publicRead;
}
