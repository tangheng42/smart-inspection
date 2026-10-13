package com.zhixunyun.service.impl;

import com.zhixunyun.context.BaseContext;
import com.zhixunyun.entity.FileObject;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.enumeration.FileBizType;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.mapper.AttachmentMapper;
import com.zhixunyun.properties.MinioProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceImplTest {

    private final MinioClient minioClient = mock(MinioClient.class);
    private final AttachmentMapper attachmentMapper = mock(AttachmentMapper.class);
    private final MinioProperties properties = properties();
    private final FileServiceImpl service = new FileServiceImpl(
            minioClient, properties, attachmentMapper);

    @AfterEach
    void tearDown() {
        BaseContext.remove();
    }

    @Test
    void shouldDetectPngByContentAndSaveMetadata() throws Exception {
        BaseContext.setCurrentId(2L);
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47,
                0x0d, 0x0a, 0x1a, 0x0a, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "scene.png", "application/octet-stream", png);
        when(minioClient.bucketExists(any())).thenReturn(true);

        var result = service.uploadImage(file, FileBizType.FAULT);

        assertThat(result.getContentType()).isEqualTo("image/png");
        assertThat(result.getUrl()).startsWith("http://localhost:9000/smart-inspection/fault/");
        verify(minioClient).putObject(any());
        verify(attachmentMapper).insertFile(any(FileObject.class));
    }

    @Test
    void shouldRejectFakeImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not-image".getBytes());

        assertError(ErrorCode.FILE_INVALID,
                () -> service.uploadImage(file, FileBizType.INSPECTION));
    }

    @Test
    void shouldRejectImageOverTenMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[10 * 1024 * 1024 + 1]);

        assertError(ErrorCode.FILE_TOO_LARGE,
                () -> service.uploadImage(file, FileBizType.REPAIR));
    }

    private MinioProperties properties() {
        MinioProperties value = new MinioProperties();
        value.setEndpoint("http://localhost:9000");
        value.setPublicUrl("http://localhost:9000");
        value.setAccessKey("minioadmin");
        value.setSecretKey("minioadmin");
        value.setBucketName("smart-inspection");
        value.setPublicRead(true);
        return value;
    }

    private void assertError(ErrorCode errorCode, Runnable operation) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(operation::run)
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(errorCode));
    }
}
