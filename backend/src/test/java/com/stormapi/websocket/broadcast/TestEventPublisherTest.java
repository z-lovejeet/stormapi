package com.stormapi.websocket.broadcast;

import com.stormapi.websocket.dto.TestEventMessage;
import com.stormapi.websocket.dto.TestEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TestEventPublisher Unit Tests")
class TestEventPublisherTest {

    private SimpMessagingTemplate messagingTemplate;
    private TestEventPublisher publisher;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        publisher = new TestEventPublisher(messagingTemplate);
    }

    @Test
    @DisplayName("publishEvent — sends to correct topic with correct payload")
    void publishEvent_sendsCorrectly() {
        publisher.publishEvent(42L, TestEventType.TEST_STARTED,
                "Test started", Map.of("resultId", 100L));

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TestEventMessage> msgCaptor = ArgumentCaptor.forClass(TestEventMessage.class);
        verify(messagingTemplate).convertAndSend(destCaptor.capture(), msgCaptor.capture());

        assertEquals("/topic/events/42", destCaptor.getValue());
        TestEventMessage msg = msgCaptor.getValue();
        assertEquals(42L, msg.testId());
        assertEquals("TEST_STARTED", msg.eventType());
        assertEquals("Test started", msg.message());
        assertEquals(100L, msg.metadata().get("resultId"));
        assertNotNull(msg.timestamp());
    }

    @Test
    @DisplayName("publishEvent — null metadata defaults to empty map")
    void publishEvent_nullMetadata() {
        publisher.publishEvent(1L, TestEventType.TEST_COMPLETED, "Done", null);

        ArgumentCaptor<TestEventMessage> msgCaptor = ArgumentCaptor.forClass(TestEventMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), msgCaptor.capture());

        assertNotNull(msgCaptor.getValue().metadata());
        assertTrue(msgCaptor.getValue().metadata().isEmpty());
    }

    @Test
    @DisplayName("publishEvent — exception does not propagate")
    void publishEvent_exceptionSafe() {
        doThrow(new RuntimeException("Connection lost"))
                .when(messagingTemplate).convertAndSend(anyString(), any(TestEventMessage.class));

        assertDoesNotThrow(() ->
                publisher.publishEvent(1L, TestEventType.TEST_FAILED, "Failed", Map.of()));
    }

}
