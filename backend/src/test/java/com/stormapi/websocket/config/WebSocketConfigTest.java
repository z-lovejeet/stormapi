package com.stormapi.websocket.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("WebSocket Configuration Tests")
class WebSocketConfigTest {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TaskScheduler webSocketHeartbeatScheduler;

    @Test
    @DisplayName("SimpMessagingTemplate bean is available")
    void messagingTemplate_available() {
        assertNotNull(messagingTemplate, "SimpMessagingTemplate should be auto-configured");
    }

    @Test
    @DisplayName("WebSocket heartbeat scheduler bean is available")
    void heartbeatScheduler_available() {
        assertNotNull(webSocketHeartbeatScheduler, "TaskScheduler should be configured");
    }

}
