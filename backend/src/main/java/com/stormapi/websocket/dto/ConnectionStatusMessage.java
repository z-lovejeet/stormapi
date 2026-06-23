package com.stormapi.websocket.dto;

import java.time.Instant;

/**
 * WebSocket DTO for connection status notifications.
 * Used by the frontend to track WebSocket connection health.
 */
public record ConnectionStatusMessage(
        String status,
        String sessionId,
        int activeTests,
        String timestamp
) {

    public static ConnectionStatusMessage connected(String sessionId, int activeTests) {
        return new ConnectionStatusMessage("CONNECTED", sessionId, activeTests,
                Instant.now().toString());
    }

    public static ConnectionStatusMessage disconnected(String sessionId) {
        return new ConnectionStatusMessage("DISCONNECTED", sessionId, 0,
                Instant.now().toString());
    }

}
