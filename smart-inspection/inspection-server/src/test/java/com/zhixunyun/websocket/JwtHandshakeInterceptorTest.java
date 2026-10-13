package com.zhixunyun.websocket;

import java.util.HashMap;
import java.util.Map;

import com.zhixunyun.properties.JwtProperties;
import com.zhixunyun.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtHandshakeInterceptorTest {

    private static final String SECRET =
            "smart-inspection-test-secret-key-123456789";

    @Test
    void validTokenShouldBindUserToWebSocketSession() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey(SECRET);
        properties.setTtl(60_000L);
        String token = JwtUtil.createJWT(SECRET, 60_000L,
                Map.of("userId", 3L, "roleCode", "MAINTAINER"));
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setScheme("ws");
        servletRequest.setServerName("localhost");
        servletRequest.setRequestURI("/ws/notifications");
        servletRequest.setQueryString("token=" + token);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = new JwtHandshakeInterceptor(properties)
                .beforeHandshake(request, response,
                        mock(WebSocketHandler.class), attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes).containsEntry("userId", 3L)
                .containsEntry("roleCode", "MAINTAINER");
    }

    @Test
    void missingTokenShouldRejectHandshake() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey(SECRET);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setScheme("ws");
        servletRequest.setServerName("localhost");
        servletRequest.setRequestURI("/ws/notifications");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        boolean accepted = new JwtHandshakeInterceptor(properties)
                .beforeHandshake(
                        new ServletServerHttpRequest(servletRequest),
                        response, mock(WebSocketHandler.class), new HashMap<>());

        assertThat(accepted).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}
