package com.zhixunyun.interceptor;

import java.util.Arrays;

import com.zhixunyun.annotation.RequireRole;
import com.zhixunyun.context.BaseContext;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 根据 Controller 方法上的 {@code @RequireRole} 限制接口角色。
 * 这里只判断调用资格；具体任务、工单等数据归属仍由业务层核验。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Object handler) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole =
                handlerMethod.getMethodAnnotation(RequireRole.class);

        if (requireRole == null) {
            return true;
        }

        String[] requiredRoles = requireRole.value();

        String currentRole = BaseContext.getCurrentRole();

        boolean allowed = Arrays.asList(requiredRoles)
                .contains(currentRole);

        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return true;
    }
}
