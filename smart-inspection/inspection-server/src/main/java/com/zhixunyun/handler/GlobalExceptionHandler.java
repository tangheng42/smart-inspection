package com.zhixunyun.handler;

import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 将业务异常和请求参数异常映射为统一响应及对应 HTTP 状态。
 * 未预期异常只记录服务端日志，对客户端返回通用系统错误。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {

        ErrorCode errorCode = ex.getErrorCode();

        HttpStatus httpStatus = getHttpStatus(errorCode);

        Result<Void> result = Result.error(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        return ResponseEntity
                .status(httpStatus)
                .body(result);
    }

    private HttpStatus getHttpStatus(ErrorCode errorCode) {

        return switch (errorCode) {
            case USERNAME_OR_PASSWORD_ERROR,
                 TOKEN_MISSING,
                 TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;

            case USER_DISABLED,
                 FORBIDDEN -> HttpStatus.FORBIDDEN;

            case PARAM_VALIDATION_FAILED,
                 INSPECTION_ITEM_RESULT_CONFIG_INVALID,
                 EQUIPMENT_STATUS_INVALID,
                 INSPECTION_ITEM_DISABLED,
                 INSPECTION_PLAN_EQUIPMENT_TYPE_MISMATCH,
                 INSPECTION_PLAN_ITEM_TYPE_MISMATCH,
                 INSPECTION_PLAN_INSPECTOR_UNAVAILABLE,
                 INSPECTION_PLAN_SCHEDULE_INVALID,
                 INSPECTION_PLAN_NO_FUTURE_EXECUTION -> HttpStatus.BAD_REQUEST;

            case AREA_EXIST_CODE,
                 AREA_HAS_EQUIPMENT,
                 USERNAME_EXISTS,
                 CANNOT_DISABLE_SELF,
                 EQUIPMENT_TYPE_CODE_EXISTS,
                 EQUIPMENT_TYPE_HAS_ENABLED_EQUIPMENT,
                 EQUIPMENT_CODE_EXISTS,
                 EQUIPMENT_SERIAL_NUMBER_EXISTS,
                 EQUIPMENT_TYPE_UNAVAILABLE,
                 EQUIPMENT_AREA_UNAVAILABLE,
                 EQUIPMENT_REFERENCED,
                 EQUIPMENT_STATUS_CHANGE_FORBIDDEN,
                 INSPECTION_ITEM_CODE_EXISTS,
                 INSPECTION_ITEM_TYPE_UNAVAILABLE,
                 INSPECTION_PLAN_NAME_EXISTS,
                 INSPECTION_PLAN_CONFLICT,
                 INSPECTION_PLAN_HAS_TASKS,
                 INSPECTION_TASK_STATUS_CONFLICT,
                 INSPECTION_TASK_ALREADY_STARTED,
                 WORK_ORDER_STATUS_CONFLICT -> HttpStatus.CONFLICT;

            case FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;

            case USER_NOT_FOUND,
                 AREA_NOT_FOUND,
                 EQUIPMENT_TYPE_NOT_FOUND,
                 EQUIPMENT_NOT_FOUND,
                 INSPECTION_ITEM_NOT_FOUND,
                 INSPECTION_PLAN_NOT_FOUND,
                 INSPECTION_TASK_NOT_FOUND,
                 INSPECTION_RECORD_NOT_FOUND,
                 FAULT_RECORD_NOT_FOUND,
                 WORK_ORDER_NOT_FOUND,
                 NOTIFICATION_NOT_FOUND -> HttpStatus.NOT_FOUND;

            case SYSTEM_ERROR,
                 FILE_STORAGE_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;

            default -> HttpStatus.BAD_REQUEST;

        };
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        Result<Void> result = Result.error(
                ErrorCode.PARAM_VALIDATION_FAILED.getCode(),
                message
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @ExceptionHandler({BindException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> handleRequestParameterException(Exception ex) {
        Result<Void> result = Result.error(
                ErrorCode.PARAM_VALIDATION_FAILED.getCode(),
                "请求参数格式错误或缺少必填参数"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeException(
            MaxUploadSizeExceededException ex) {
        Result<Void> result = Result.error(
                ErrorCode.FILE_TOO_LARGE.getCode(),
                ErrorCode.FILE_TOO_LARGE.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception ex) {
        log.error("未处理的系统异常", ex);
        Result<Void> result = Result.error(
                ErrorCode.SYSTEM_ERROR.getCode(),
                ErrorCode.SYSTEM_ERROR.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

}
