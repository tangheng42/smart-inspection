package com.zhixunyun.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixunyun.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 按用户维护本进程中的 WebSocket 会话集合，支持同一用户多个连接。
 * 离线或发送失败时不补发；持久化通知仍可通过通知查询接口获取。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions =
            new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
        session.sendMessage(new TextMessage(
                "{\"type\":\"CONNECTED\",\"message\":\"WebSocket连接成功\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("WebSocket传输异常，sessionId={}", session.getId(), exception);
        remove(session);
    }

    public void send(NotificationEvent event) {
        Set<WebSocketSession> userSessions = sessions.get(event.recipientId());
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(event));
            for (WebSocketSession session : Set.copyOf(userSessions)) {
                if (!session.isOpen()) {
                    remove(session);
                    continue;
                }
                try {
                    session.sendMessage(message);
                } catch (IOException ex) {
                    log.warn("WebSocket推送失败，userId={}, sessionId={}",
                            event.recipientId(), session.getId(), ex);
                    remove(session);
                }
            }
        } catch (Exception ex) {
            log.warn("通知事件序列化失败，eventId={}", event.eventId(), ex);
        }
    }

    private void remove(WebSocketSession session) {
        Object userIdValue = session.getAttributes().get("userId");
        if (!(userIdValue instanceof Long userId)) {
            return;
        }
        sessions.computeIfPresent(userId, (ignored, values) -> {
            values.remove(session);
            return values.isEmpty() ? null : values;
        });
    }
}
