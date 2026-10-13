package com.zhixunyun.handler;

import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void inspectionTaskStatusConflictShouldReturnHttp409() {
        var response = handler.handleBusinessException(
                new BusinessException(ErrorCode.INSPECTION_TASK_STATUS_CONFLICT));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ErrorCode.INSPECTION_TASK_STATUS_CONFLICT.getCode());
    }

    @Test
    void workOrderStatusConflictShouldReturnHttp409() {
        var response = handler.handleBusinessException(
                new BusinessException(ErrorCode.WORK_ORDER_STATUS_CONFLICT));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void fileStorageFailureShouldReturnHttp500() {
        var response = handler.handleBusinessException(
                new BusinessException(ErrorCode.FILE_STORAGE_ERROR));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
