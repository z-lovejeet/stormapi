package com.stormapi.websocket.dto;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket DTO for test lifecycle events.
 * Sent to /topic/events/{testId} on state transitions.
 */
public record TestEventMessage(
        long testId,
        String eventType,
        String message,
        Map<String, Object> metadata,
        String timestamp
) {

    public static TestEventMessage of(long testId, TestEventType type,
                                       String message, Map<String, Object> metadata) {
        return new TestEventMessage(
                testId,
                type.name(),
                message,
                metadata != null ? metadata : Map.of(),
                Instant.now().toString()
        );
    }

}
