package com.zhixunyun.service.impl;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import com.zhixunyun.context.BaseContext;
import com.zhixunyun.entity.FileObject;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.FileBizType;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.AttachmentMapper;
import com.zhixunyun.properties.MinioProperties;
import com.zhixunyun.service.FileService;
import com.zhixunyun.vo.FileUploadVO;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 校验图片实际文件格式后上传 MinIO，并保存文件归属供业务附件引用。
 * 对象上传成功而数据库写入失败时尝试删除对象；两种存储之间没有分布式事务。
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final AttachmentMapper attachmentMapper;
    private volatile boolean bucketReady;

    @Override
    public FileUploadVO uploadImage(MultipartFile file, FileBizType bizType) {
        if (file == null || file.isEmpty() || bizType == null) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
        ImageFormat format = detectFormat(bytes);
        String originalName = normalizeOriginalName(file.getOriginalFilename(), format.extension());
        String objectKey = buildObjectKey(bizType, format.extension());
        String url = trimTrailingSlash(properties.getPublicUrl()) + "/"
                + properties.getBucketName() + "/" + objectKey;
        boolean uploaded = false;

        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L)
                    .contentType(format.contentType())
                    .build());
            uploaded = true;

            FileObject fileObject = new FileObject();
            fileObject.setBucketName(properties.getBucketName());
            fileObject.setObjectKey(objectKey);
            fileObject.setUrl(url);
            fileObject.setOriginalName(originalName);
            fileObject.setContentType(format.contentType());
            fileObject.setSizeBytes((long) bytes.length);
            fileObject.setUploaderId(BaseContext.getCurrentId());
            attachmentMapper.insertFile(fileObject);

            return new FileUploadVO(objectKey, url, originalName,
                    format.contentType(), (long) bytes.length);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            if (uploaded) {
                removeQuietly(objectKey);
            }
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucketName()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getBucketName()).build());
        }
        if (properties.isPublicRead()) {
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(properties.getBucketName())
                    .config(publicReadPolicy(properties.getBucketName()))
                    .build());
        }
        bucketReady = true;
    }

    private String publicReadPolicy(String bucket) {
        return """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow",
                "Principal":{"AWS":["*"]},"Action":["s3:GetObject"],
                "Resource":["arn:aws:s3:::%s/*"]}]}
                """.formatted(bucket).replace("\n", "");
    }

    private void removeQuietly(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectKey)
                    .build());
        } catch (Exception ignored) {
            // 这里只能尽力清理；若删除也失败，可能遗留未关联的对象。
        }
    }

    private ImageFormat detectFormat(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return new ImageFormat("image/jpeg", ".jpg");
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a
                && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return new ImageFormat("image/png", ".png");
        }
        if (bytes.length >= 12
                && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return new ImageFormat("image/webp", ".webp");
        }
        throw new BusinessException(ErrorCode.FILE_INVALID);
    }

    private String normalizeOriginalName(String originalName, String extension) {
        String value = originalName == null ? "image" + extension
                : originalName.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        if (value.isEmpty()) {
            value = "image" + extension;
        }
        if (value.length() > 255) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        return value;
    }

    private String buildObjectKey(FileBizType bizType, String extension) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return bizType.name().toLowerCase(Locale.ROOT) + "/" + date + "/"
                + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record ImageFormat(String contentType, String extension) {
    }
}
