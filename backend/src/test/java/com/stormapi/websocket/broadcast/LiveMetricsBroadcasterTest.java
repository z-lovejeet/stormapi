package com.stormapi.websocket.broadcast;

import com.stormapi.engine.metrics.LiveMetricsSnapshot;
import com.stormapi.websocket.dto.LiveMetricsMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("LiveMetricsBroadcaster Unit Tests")
class LiveMetricsBroadcasterTest {

    private SimpMessagingTemplate messagingTemplate;
    private LiveMetricsBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        broadcaster = new LiveMetricsBroadcaster(messagingTemplate);
    }

    private LiveMetricsSnapshot createSnapshot() {
        return new LiveMetricsSnapshot(
                100, 95, 5, 50.0, 10.0, 200.0,
                45.0, 55.0, 80.0, 90.0, 150.0,
                20.0, 0.05, 10, 50000,
                Map.of(200, 95L, 500, 5L), Instant.now()
        );
    }

    @Test
    @DisplayName("broadcast — sends when active")
    void broadcast_sendsWhenActive() {
        broadcaster.startBroadcasting(42L);
        LiveMetricsSnapshot snapshot = createSnapshot();

        broadcaster.broadcast(42L, snapshot);

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LiveMetricsMessage> msgCaptor = ArgumentCaptor.forClass(LiveMetricsMessage.class);
        verify(messagingTemplate).convertAndSend(destCaptor.capture(), msgCaptor.capture());

        assertEquals("/topic/metrics/42", destCaptor.getValue());
        assertEquals(42L, msgCaptor.getValue().testId());
        assertEquals(100, msgCaptor.getValue().totalRequests());
    }

    @Test
    @DisplayName("broadcast — does nothing when not active")
    void broadcast_noopWhenInactive() {
        broadcaster.broadcast(42L, createSnapshot());

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("stopBroadcasting — sends final snapshot and removes")
    void stopBroadcasting_sendsFinalAndRemoves() {
        broadcaster.startBroadcasting(42L);
        broadcaster.stopBroadcasting(42L, createSnapshot());

        verify(messagingTemplate).convertAndSend(eq("/topic/metrics/42"), any(LiveMetricsMessage.class));
        assertFalse(broadcaster.isActive(42L));
    }

    @Test
    @DisplayName("stopBroadcasting — null snapshot is safe")
    void stopBroadcasting_nullSnapshot() {
        broadcaster.startBroadcasting(42L);
        assertDoesNotThrow(() -> broadcaster.stopBroadcasting(42L, null));
        assertFalse(broadcaster.isActive(42L));
    }

    @Test
    @DisplayName("getActiveBroadcastCount — correct count")
    void getActiveBroadcastCount_correct() {
        assertEquals(0, broadcaster.getActiveBroadcastCount());
        broadcaster.startBroadcasting(1L);
        broadcaster.startBroadcasting(2L);
        assertEquals(2, broadcaster.getActiveBroadcastCount());
        broadcaster.stopBroadcasting(1L, null);
        assertEquals(1, broadcaster.getActiveBroadcastCount());
    }

    @Test
    @DisplayName("broadcast — exception does not propagate")
    void broadcast_exceptionSafe() {
        broadcaster.startBroadcasting(42L);
        doThrow(new RuntimeException("Send failed"))
                .when(messagingTemplate).convertAndSend(anyString(), any(LiveMetricsMessage.class));

        assertDoesNotThrow(() -> broadcaster.broadcast(42L, createSnapshot()));
    }

}
