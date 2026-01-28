package com.testing.automation.config;

import com.testing.automation.controller.RecordingWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RecordingWebSocketHandler recordingWebSocketHandler;

    public WebSocketConfig(RecordingWebSocketHandler recordingWebSocketHandler) {
        this.recordingWebSocketHandler = recordingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册录制 WebSocket 端点
        registry.addHandler(recordingWebSocketHandler, "/api/ui-tests/recording/{caseId}")
                .setAllowedOriginPatterns("*");
    }
}
