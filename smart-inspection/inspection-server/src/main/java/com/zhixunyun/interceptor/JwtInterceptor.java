package com.zhixunyun.interceptor;

import com.zhixunyun.context.BaseContext;
import com.zhixunyun.enumeration.ErrorCode;
import com.zhixunyun.exception.BusinessException;
import com.zhixunyun.properties.JwtProperties;
import com.zhixunyun.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 校验 API 请求的 JWT 并将用户 ID、角色写入当前请求线程。
 * 请求结束清理 ThreadLocal，避免容器线程复用时泄漏上一请求身份。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Object handler) {

        // 浏览器跨域预检请求不携带 Token，应直接放行。
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            throw new BusinessException(
                    ErrorCode.TOKEN_MISSING
            );
        }

        String token =
                authorization.substring(7);

        try {

            Claims claims = JwtUtil.parseJWT(
                    jwtProperties.getSecretKey(),
                    token
            );

            Number userIdClaim = claims.get("userId", Number.class);

            if (userIdClaim == null) {
                throw new IllegalArgumentException("JWT 中缺少 userId");
            }

            Long userId = userIdClaim.longValue();

            String roleCode = claims.get("roleCode", String.class);

            if (roleCode == null || roleCode.isBlank()) {
                throw new IllegalArgumentException("JWT 中缺少 roleCode");
            }

            BaseContext.setCurrentId(userId);
            BaseContext.setCurrentRole(roleCode);

            return true;

        } catch (Exception e) {

            throw new BusinessException(
                    ErrorCode.TOKEN_INVALID
            );
        }
    }

    @Override
    public void afterCompletion(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Object handler,
            Exception ex) {

        BaseContext.remove();
    }
}
