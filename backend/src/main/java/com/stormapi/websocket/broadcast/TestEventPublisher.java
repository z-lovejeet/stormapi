package com.stormapi.websocket.broadcast;

import com.stormapi.websocket.dto.TestEventMessage;
import com.stormapi.websocket.dto.TestEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Publishes test lifecycle events to WebSocket subscribers.
 *
 * Fire-and-forget: no state retained. Each event is constructed
 * and immediately sent to /topic/events/{testId}.
 *
 * Thread-safe: SimpMessagingTemplate.convertAndSend() is thread-safe.
 */
@Service
public class TestEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TestEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public TestEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publishes a lifecycle event for a test.
     *
     * @param testId    the test config ID
     * @param eventType the lifecycle event type
     * @param message   human-readable description
     * @param metadata  event-specific key-value pairs (nullable)
     */
    public void publishEvent(Long testId, TestEventType eventType,
                             String message, Map<String, Object> metadata) {
        try {
            TestEventMessage event = TestEventMessage.of(testId, eventType, message, metadata);
            messagingTemplate.convertAndSend("/topic/events/" + testId, event);
            log.info("Test event: {} → {}", testId, eventType);
        } catch (Exception ex) {
            log.warn("Failed to publish event {} for test {}: {}",
                    eventType, testId, ex.getMessage());
        }
    }

}
