package com.stormapi.engine.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThroughputTracker Tests")
class ThroughputTrackerTest {

    private ThroughputTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ThroughputTracker();
    }

    @Test
    @DisplayName("no recordings returns zero RPS")
    void noRecordings_zeroRps() {
        assertEquals(0.0, tracker.getCurrentRps());
    }

    @Test
    @DisplayName("records in current second and reports from previous second")
    void knownThroughput_accurateRps() throws InterruptedException {
        // Record 100 requests in this second
        for (int i = 0; i < 100; i++) {
            tracker.record();
        }

        // Wait for the second to complete so it becomes the "previous" second
        Thread.sleep(1100);

        double rps = tracker.getCurrentRps();
        assertEquals(100.0, rps, 15.0,
                "RPS should be approximately 100, got: " + rps);
    }

    @Test
    @DisplayName("reset clears all buckets")
    void reset_clearsAllBuckets() {
        for (int i = 0; i < 50; i++) {
            tracker.record();
        }
        tracker.reset();

        assertEquals(0.0, tracker.getCurrentRps());
    }

    @Test
    @DisplayName("100 threads recording concurrently — no lost counts")
    void concurrentRecording_noLostCounts() throws InterruptedException {
        int threadCount = 100;
        int recordingsPerThread = 100;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i ->
                Thread.ofVirtual().start(() -> {
                    try {
                        startGate.await();
                        for (int j = 0; j < recordingsPerThread; j++) {
                            tracker.record();
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

        // Wait for this second to complete
        Thread.sleep(1100);

        double rps = tracker.getCurrentRps();
        // All 10,000 recordings happened in the same second (allow tolerance for scheduling jitter)
        assertEquals(10_000.0, rps, 500.0,
                "Should capture all concurrent recordings, got: " + rps);
    }

}
