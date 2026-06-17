package com.stormapi.engine.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StatusCodeTracker Tests")
class StatusCodeTrackerTest {

    private StatusCodeTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new StatusCodeTracker();
    }

    @Test
    @DisplayName("records and increments single status code count")
    void record_incrementsCount() {
        tracker.record(200);
        tracker.record(200);
        tracker.record(200);
        tracker.record(200);
        tracker.record(200);

        assertEquals(5, tracker.getCount(200));
    }

    @Test
    @DisplayName("tracks multiple status codes independently")
    void record_multipleStatusCodes() {
        for (int i = 0; i < 100; i++) tracker.record(200);
        for (int i = 0; i < 10; i++) tracker.record(404);
        for (int i = 0; i < 5; i++) tracker.record(500);

        Map<Integer, Long> dist = tracker.getDistribution();
        assertEquals(3, dist.size());
        assertEquals(100L, dist.get(200));
        assertEquals(10L, dist.get(404));
        assertEquals(5L, dist.get(500));
    }

    @Test
    @DisplayName("getDistribution returns unmodifiable map")
    void getDistribution_returnsUnmodifiableMap() {
        tracker.record(200);
        Map<Integer, Long> dist = tracker.getDistribution();

        assertThrows(UnsupportedOperationException.class, () -> dist.put(500, 1L));
    }

    @Test
    @DisplayName("reset clears all data")
    void reset_clearsAllData() {
        tracker.record(200);
        tracker.record(500);
        tracker.reset();

        assertEquals(0, tracker.getCount(200));
        assertEquals(0, tracker.getCount(500));
        assertTrue(tracker.getDistribution().isEmpty());
    }

    @Test
    @DisplayName("skips statusCode 0 (connection failures)")
    void skipZeroStatusCode() {
        tracker.record(0);
        tracker.record(0);
        tracker.record(200);

        assertEquals(0, tracker.getCount(0));
        assertEquals(1, tracker.getCount(200));
        assertFalse(tracker.getDistribution().containsKey(0));
    }

    @Test
    @DisplayName("100 threads recording concurrently — no lost updates")
    void concurrentRecording_noLostUpdates() throws InterruptedException {
        int threadCount = 100;
        int recordingsPerThread = 100;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i ->
                Thread.ofVirtual().start(() -> {
                    try {
                        startGate.await();
                        for (int j = 0; j < recordingsPerThread; j++) {
                            tracker.record(200);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                })
        );

        startGate.countDown();
        doneLatch.await();

        assertEquals(threadCount * recordingsPerThread, tracker.getCount(200),
                "All recordings should be captured without lost updates");
    }

}
