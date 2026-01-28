package com.testing.automation.controller;

import com.testing.automation.service.UiTestRecordingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RecordingWebSocketHandler extends TextWebSocketHandler {
    
    private final UiTestRecordingService recordingService;
    
    // 存储 WebSocket 会话，按 caseId 分组
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    public RecordingWebSocketHandler(UiTestRecordingService recordingService) {
        this.recordingService = recordingService;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long caseId = extractCaseId(session);
        if (caseId != null) {
            sessions.put(caseId, session);
            log.info("WebSocket connection established for case: {}", caseId);
            
            // 注册代码回调
            recordingService.registerCodeCallback(caseId, code -> {
                try {
                    if (session.isOpen()) {
                        String message = String.format("{\"type\":\"code\",\"code\":\"%s\"}", 
                            escapeJson(code));
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    log.error("Failed to send code message for case: {}", caseId, e);
                }
            });
        } else {
            log.warn("Failed to extract caseId from session URI: {}", session.getUri());
            session.close(CloseStatus.BAD_DATA);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long caseId = extractCaseId(session);
        if (caseId != null) {
            sessions.remove(caseId);
            recordingService.removeCodeCallback(caseId);
            log.info("WebSocket connection closed for case: {}, status: {}", caseId, status);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发送的消息（如果需要）
        log.debug("Received message from client: {}", message.getPayload());
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long caseId = extractCaseId(session);
        log.error("WebSocket transport error for case: {}", caseId, exception);
        if (caseId != null) {
            sessions.remove(caseId);
            recordingService.removeCodeCallback(caseId);
        }
    }
    
    /**
     * 从 WebSocket URI 中提取 caseId
     * URI 格式: /api/ui-tests/recording/{caseId}
     */
    private Long extractCaseId(WebSocketSession session) {
        try {
            String path = session.getUri().getPath();
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("recording".equals(parts[i]) && i + 1 < parts.length) {
                    return Long.parseLong(parts[i + 1]);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract caseId from URI: {}", session.getUri(), e);
        }
        return null;
    }
    
    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
