package com.zhixunyun.aspect;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixunyun.annotation.OperationLog;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.entity.SysOperationLog;
import com.zhixunyun.entity.User;
import com.zhixunyun.mapper.OperationLogMapper;
import com.zhixunyun.mapper.UserMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogAspectTest {

    @AfterEach
    void clear() {
        BaseContext.remove();
    }

    @Test
    void shouldRecordSuccessAndMaskSensitiveFields() throws Throwable {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        User user = new User();
        user.setDisplayName("系统管理员");

        BaseContext.setCurrentId(1L);
        when(userMapper.getById(1L)).thenReturn(user);
        when(joinPoint.proceed()).thenReturn("ok");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                LoggedMethods.class.getDeclaredMethod("resetPassword", Map.class));
        when(signature.getDeclaringTypeName()).thenReturn("DemoService");
        when(signature.getName()).thenReturn("resetPassword");
        when(joinPoint.getArgs()).thenReturn(new Object[]{
                Map.of("newPassword", "123456", "remark", "test")});
        Object result = new OperationLogAspect(mapper, userMapper, new ObjectMapper())
                .record(joinPoint);

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<SysOperationLog> captor =
                ArgumentCaptor.forClass(SysOperationLog.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getSuccess()).isEqualTo(1);
        assertThat(captor.getValue().getRequestParams())
                .contains("***").doesNotContain("123456");
    }

    private static class LoggedMethods {
        @OperationLog(value = "重置密码", businessType = "USER")
        @SuppressWarnings("unused")
        void resetPassword(Map<String, String> request) {
        }
    }
}
