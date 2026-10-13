package com.zhixunyun.aspect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhixunyun.annotation.OperationLog;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.entity.SysOperationLog;
import com.zhixunyun.entity.User;
import com.zhixunyun.mapper.OperationLogMapper;
import com.zhixunyun.mapper.UserMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.aop.support.AopUtils;

/**
 * 对标记的业务方法记录成功或异常、耗时及脱敏后的请求参数。
 * 记录失败不会覆盖原业务结果；当前实现在方法调用结束时直接写库。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
        prefix = "zhixunyun.operation-log", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class OperationLogAspect {

    private static final List<String> SENSITIVE_WORDS = List.of(
            "password", "token", "secret", "authorization");

    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /** 环绕业务方法，保留原异常并在 finally 中尝试记录操作结果。 */
    @Around("@annotation(com.zhixunyun.annotation.OperationLog)")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        OperationLog operationLog = resolveAnnotation(joinPoint);
        long start = System.nanoTime();
        Throwable failure = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            saveSafely(joinPoint, operationLog, failure, durationMs);
        }
    }

    private OperationLog resolveAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (joinPoint.getTarget() != null) {
            method = AopUtils.getMostSpecificMethod(
                    method, joinPoint.getTarget().getClass());
        }
        OperationLog annotation = AnnotatedElementUtils.findMergedAnnotation(
                method, OperationLog.class);
        if (annotation == null) {
            throw new IllegalStateException("未找到OperationLog注解");
        }
        return annotation;
    }

    private void saveSafely(
            ProceedingJoinPoint joinPoint,
            OperationLog annotation,
            Throwable failure,
            long durationMs) {
        try {
            HttpServletRequest request = currentRequest();
            Long operatorId = BaseContext.getCurrentId();
            User user = operatorId == null ? null : userMapper.getById(operatorId);

            SysOperationLog record = new SysOperationLog();
            record.setTraceId(UUID.randomUUID().toString());
            record.setOperatorId(operatorId);
            record.setOperatorName(user == null ? null : truncate(user.getDisplayName(), 50));
            record.setOperationName(annotation.value());
            record.setBusinessType(annotation.businessType());
            record.setRequestMethod(request == null ? null : request.getMethod());
            record.setRequestUri(request == null ? null : truncate(request.getRequestURI(), 255));
            record.setJavaMethod(truncate(javaMethod(joinPoint), 500));
            record.setRequestParams(serializeArguments(joinPoint.getArgs()));
            record.setSuccess(failure == null ? 1 : 0);
            record.setErrorMessage(failure == null ? null
                    : truncate(failure.getMessage(), 1000));
            record.setDurationMs(durationMs);
            record.setIpAddress(request == null ? null : clientIp(request));
            operationLogMapper.insert(record);
        } catch (Exception logFailure) {
            // 日志是旁路能力，不能因为日志失败改变核心业务结果。
            log.warn("保存操作日志失败", logFailure);
        }
    }

    private String serializeArguments(Object[] args) {
        try {
            List<Object> safeArgs = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
                    continue;
                }
                if (arg instanceof MultipartFile file) {
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("originalName", file.getOriginalFilename());
                    metadata.put("contentType", file.getContentType());
                    metadata.put("size", file.getSize());
                    safeArgs.add(metadata);
                } else {
                    safeArgs.add(arg);
                }
            }
            JsonNode root = objectMapper.valueToTree(safeArgs);
            desensitize(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            return "[参数序列化失败]";
        }
    }

    private void desensitize(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> names = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                if (isSensitive(name)) {
                    objectNode.put(name, "***");
                } else {
                    desensitize(objectNode.get(name));
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::desensitize);
        }
    }

    private boolean isSensitive(String fieldName) {
        String lower = fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_WORDS.stream().anyMatch(lower::contains);
    }

    private String javaMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 64);
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
