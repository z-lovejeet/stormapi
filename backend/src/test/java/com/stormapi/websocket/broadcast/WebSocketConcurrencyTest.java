package com.stormapi.websocket.broadcast;

import com.stormapi.engine.http.RequestResult;
import com.stormapi.engine.metrics.LiveMetricsSnapshot;
import com.stormapi.websocket.session.WebSocketSessionTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WebSocket Concurrency Tests")
class WebSocketConcurrencyTest {

    @Test
    @DisplayName("RequestLogBroadcaster — concurrent capture from 50 virtual threads")
    void requestLogBroadcaster_concurrentCapture() throws InterruptedException {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        RequestLogBroadcaster broadcaster = new RequestLogBroadcaster(template);
        broadcaster.startCapturing(1L);

        int threadCount = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 10; j++) {
                            broadcaster.captureResult(1L,
                                    new RequestResult(200, 50_000_000L, 1024L, true, null, Instant.now()),
                                    "http://example.com", "GET");
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
        }

        // Should not throw, should not corrupt state
        assertDoesNotThrow(() -> broadcaster.flush(1L));
        assertDoesNotThrow(() -> broadcaster.stopCapturing(1L));
    }

    @Test
    @DisplayName("LiveMetricsBroadcaster — concurrent broadcast from multiple threads")
    void liveMetricsBroadcaster_concurrentBroadcast() throws InterruptedException {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        LiveMetricsBroadcaster broadcaster = new LiveMetricsBroadcaster(template);

        int testCount = 10;
        for (long i = 1; i <= testCount; i++) {
            broadcaster.startBroadcasting(i);
        }

        CountDownLatch latch = new CountDownLatch(testCount);
        LiveMetricsSnapshot snapshot = new LiveMetricsSnapshot(
                100, 95, 5, 50.0, 10.0, 200.0,
                45.0, 55.0, 80.0, 90.0, 150.0,
                20.0, 0.05, 10, 50000,
                Map.of(200, 95L), Instant.now()
        );

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (long i = 1; i <= testCount; i++) {
                final long testId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 20; j++) {
                            broadcaster.broadcast(testId, snapshot);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        }

        assertEquals(testCount, broadcaster.getActiveBroadcastCount());

        // Stop all
        for (long i = 1; i <= testCount; i++) {
            broadcaster.stopBroadcasting(i, null);
        }
        assertEquals(0, broadcaster.getActiveBroadcastCount());
    }

    @Test
    @DisplayName("WebSocketSessionTracker — concurrent register/remove")
    void sessionTracker_concurrentAccess() throws InterruptedException {
        WebSocketSessionTracker tracker = new WebSocketSessionTracker();

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                final int id = i;
                executor.submit(() -> {
                    try {
                        String sessionId = "session-" + id;
                        tracker.registerSession(sessionId, "127.0.0.1");
                        tracker.addSubscription(sessionId, "/topic/metrics/" + (id % 5));
                        // Half the threads remove their sessions
                        if (id % 2 == 0) {
                            tracker.removeSession(sessionId);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        }

        // Half should remain
        assertEquals(threadCount / 2, tracker.getActiveSessionCount());
    }

}
