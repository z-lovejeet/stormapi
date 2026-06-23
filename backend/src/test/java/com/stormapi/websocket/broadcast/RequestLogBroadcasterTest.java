package com.stormapi.websocket.broadcast;

import com.stormapi.engine.http.RequestResult;
import com.stormapi.websocket.dto.RequestLogMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RequestLogBroadcaster Unit Tests")
class RequestLogBroadcasterTest {

    private SimpMessagingTemplate messagingTemplate;
    private RequestLogBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        broadcaster = new RequestLogBroadcaster(messagingTemplate);
    }

    private RequestResult createResult(int statusCode, long responseTimeNanos) {
        return new RequestResult(
                statusCode, responseTimeNanos, 1024L, true,
                null, Instant.now()
        );
    }

    @Test
    @DisplayName("captureResult + flush — captures and broadcasts batch")
    void captureAndFlush_broadcasts() {
        broadcaster.startCapturing(42L);

        broadcaster.captureResult(42L, createResult(200, 50_000_000L), "http://example.com", "GET");
        broadcaster.captureResult(42L, createResult(200, 60_000_000L), "http://example.com", "GET");
        broadcaster.flush(42L);

        ArgumentCaptor<RequestLogMessage> msgCaptor = ArgumentCaptor.forClass(RequestLogMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/logs/42"), msgCaptor.capture());

        RequestLogMessage msg = msgCaptor.getValue();
        assertEquals(42L, msg.testId());
        assertEquals(2, msg.entries().size());
    }

    @Test
    @DisplayName("flush — empty buffer does not broadcast")
    void flush_emptyBuffer_noOp() {
        broadcaster.startCapturing(42L);
        broadcaster.flush(42L);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("captureResult — not active is no-op")
    void captureResult_notActive_noOp() {
        broadcaster.captureResult(42L, createResult(200, 50_000_000L), "http://example.com", "GET");
        broadcaster.flush(42L);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("buffer eviction — oldest entries evicted when exceeding MAX_BUFFER_SIZE")
    void bufferEviction_evictsOldest() {
        broadcaster.startCapturing(42L);

        // Fill beyond MAX_BUFFER_SIZE
        for (int i = 0; i < RequestLogBroadcaster.MAX_BUFFER_SIZE + 20; i++) {
            broadcaster.captureResult(42L, createResult(200, (long) i * 1_000_000), "http://example.com", "GET");
        }

        broadcaster.flush(42L);

        ArgumentCaptor<RequestLogMessage> msgCaptor = ArgumentCaptor.forClass(RequestLogMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/logs/42"), msgCaptor.capture());

        // Should flush BATCH_SIZE entries (50)
        assertTrue(msgCaptor.getValue().entries().size() <= RequestLogBroadcaster.BATCH_SIZE);
    }

    @Test
    @DisplayName("createConsumer — returns functional consumer")
    void createConsumer_functional() {
        broadcaster.startCapturing(42L);

        Consumer<RequestResult> consumer = broadcaster.createConsumer(42L, "http://example.com", "POST");
        consumer.accept(createResult(201, 30_000_000L));
        broadcaster.flush(42L);

        verify(messagingTemplate).convertAndSend(eq("/topic/logs/42"), any(RequestLogMessage.class));
    }

    @Test
    @DisplayName("stopCapturing — drains remaining and broadcasts")
    void stopCapturing_drains() {
        broadcaster.startCapturing(42L);
        broadcaster.captureResult(42L, createResult(200, 50_000_000L), "http://example.com", "GET");
        broadcaster.stopCapturing(42L);

        verify(messagingTemplate).convertAndSend(eq("/topic/logs/42"), any(RequestLogMessage.class));
        assertFalse(broadcaster.isActive(42L));
    }

    @Test
    @DisplayName("stopCapturing — empty buffer does not broadcast")
    void stopCapturing_emptyBuffer() {
        broadcaster.startCapturing(42L);
        broadcaster.stopCapturing(42L);

        verifyNoInteractions(messagingTemplate);
        assertFalse(broadcaster.isActive(42L));
    }

    @Test
    @DisplayName("flush — exception does not propagate")
    void flush_exceptionSafe() {
        broadcaster.startCapturing(42L);
        broadcaster.captureResult(42L, createResult(200, 50_000_000L), "http://example.com", "GET");
        doThrow(new RuntimeException("Send failed"))
                .when(messagingTemplate).convertAndSend(anyString(), any(RequestLogMessage.class));

        assertDoesNotThrow(() -> broadcaster.flush(42L));
    }

}
