package com.stormapi.engine.metrics;

import com.stormapi.engine.http.RequestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetricsCollector Tests")
class MetricsCollectorTest {

    private MetricsCollector collector;
    private AtomicInteger activeUsers;

    @BeforeEach
    void setUp() {
        activeUsers = new AtomicInteger(0);
        collector = new MetricsCollector(activeUsers::get);
    }

    // ── Basic recording ────────────────────────────────────────────

    @Test
    @DisplayName("records single success result and updates all counters")
    void recordSingleResult_updatesAllCounters() {
        RequestResult result = RequestResult.success(200, 50_000_000L, 1024, Instant.now());
        collector.recordResult(result);

        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(1, snap.totalRequests());
        assertEquals(1, snap.successCount());
        assertEquals(0, snap.failureCount());
        assertEquals(1024, snap.totalDataBytes());
        assertTrue(snap.avgResponseTimeMs() > 0);
    }

    @Test
    @DisplayName("records failure and increments failure count")
    void recordFailure_incrementsFailureCount() {
        RequestResult result = RequestResult.failure("Connection refused", 10_000_000L, Instant.now());
        collector.recordResult(result);

        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(1, snap.totalRequests());
        assertEquals(0, snap.successCount());
        assertEquals(1, snap.failureCount());
    }

    // ── Snapshot correctness ───────────────────────────────────────

    @Test
    @DisplayName("snapshot returns consistent data for known inputs")
    void snapshot_returnsConsistentData() {
        // Record 100 successes with 50ms latency each
        for (int i = 0; i < 100; i++) {
            collector.recordResult(
                    RequestResult.success(200, 50_000_000L, 512, Instant.now()));
        }

        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(100, snap.totalRequests());
        assertEquals(100, snap.successCount());
        assertEquals(0, snap.failureCount());
        assertEquals(50.0, snap.avgResponseTimeMs(), 1.0);
        assertEquals(100 * 512L, snap.totalDataBytes());
        assertEquals(0.0, snap.errorRate(), 0.01);
    }

    @Test
    @DisplayName("snapshot includes correct status code distribution")
    void snapshot_statusCodeDistribution() {
        for (int i = 0; i < 80; i++) {
            collector.recordResult(
                    RequestResult.success(200, 10_000_000L, 100, Instant.now()));
        }
        for (int i = 0; i < 20; i++) {
            collector.recordResult(
                    RequestResult.success(500, 10_000_000L, 50, Instant.now()));
        }

        LiveMetricsSnapshot snap = collector.snapshot();
        Map<Integer, Long> dist = snap.statusCodeDistribution();
        assertEquals(2, dist.size());
        assertEquals(80L, dist.get(200));
        assertEquals(20L, dist.get(500));
    }

    @Test
    @DisplayName("snapshot includes accurate percentiles for known latencies")
    void snapshot_percentiles() {
        // Record 1ms through 100ms
        for (int ms = 1; ms <= 100; ms++) {
            collector.recordResult(
                    RequestResult.success(200, ms * 1_000_000L, 100, Instant.now()));
        }

        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(50.0, snap.p50Ms(), 2.0);
        assertEquals(75.0, snap.p75Ms(), 2.0);
        assertEquals(90.0, snap.p90Ms(), 2.0);
        assertEquals(95.0, snap.p95Ms(), 2.0);
        assertEquals(99.0, snap.p99Ms(), 2.0);
    }

    // ── Reset ──────────────────────────────────────────────────────

    @Test
    @DisplayName("reset clears all metrics back to initial state")
    void reset_clearsEverything() {
        // Record some data
        for (int i = 0; i < 50; i++) {
            collector.recordResult(
                    RequestResult.success(200, 10_000_000L, 100, Instant.now()));
        }
        collector.recordResult(
                RequestResult.failure("timeout", 5_000_000L, Instant.now()));

        // Reset
        collector.reset();

        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(0, snap.totalRequests());
        assertEquals(0, snap.successCount());
        assertEquals(0, snap.failureCount());
        assertEquals(0.0, snap.avgResponseTimeMs());
        assertEquals(0.0, snap.minResponseTimeMs());
        assertEquals(0.0, snap.maxResponseTimeMs());
        assertEquals(0, snap.totalDataBytes());
        assertTrue(snap.statusCodeDistribution().isEmpty());
    }

    // ── Edge cases ─────────────────────────────────────────────────

    @Test
    @DisplayName("error rate is 0 when no results recorded")
    void errorRate_zeroDivision() {
        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(0.0, snap.errorRate());
    }

    @Test
    @DisplayName("active users reads from supplier")
    void activeUsers_readsFromSupplier() {
        activeUsers.set(42);
        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(42, snap.activeUsers());
    }

    @Test
    @DisplayName("connection failures do not appear in status code distribution")
    void connectionFailure_statusCodeNotTracked() {
        collector.recordResult(
                RequestResult.failure("Connection refused", 5_000_000L, Instant.now()));

        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(1, snap.failureCount());
        assertFalse(snap.statusCodeDistribution().containsKey(0));
        assertTrue(snap.statusCodeDistribution().isEmpty());
    }

    @Test
    @DisplayName("error rate is correct percentage")
    void errorRate_computedCorrectly() {
        // 90 successes, 10 failures = 10% error rate
        for (int i = 0; i < 90; i++) {
            collector.recordResult(
                    RequestResult.success(200, 10_000_000L, 100, Instant.now()));
        }
        for (int i = 0; i < 10; i++) {
            collector.recordResult(
                    RequestResult.failure("error", 10_000_000L, Instant.now()));
        }

        LiveMetricsSnapshot snap = collector.snapshot();
        assertEquals(10.0, snap.errorRate(), 0.1);
    }

    // ── Concurrency ────────────────────────────────────────────────

    @Test
    @DisplayName("10,000 virtual threads recording 100 results each — zero data loss")
    void concurrency_10kThreads_noLostUpdates() throws InterruptedException {
        int threadCount = 10_000;
        int recordingsPerThread = 100;
        int expectedTotal = threadCount * recordingsPerThread; // 1,000,000

        // 70% success, 30% failure per thread
        int successPerThread = 70;
        int failurePerThread = 30;
        int expectedSuccess = threadCount * successPerThread;
        int expectedFailure = threadCount * failurePerThread;

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i ->
                Thread.ofVirtual().start(() -> {
                    try {
                        startGate.await();
                        for (int j = 0; j < successPerThread; j++) {
                            collector.recordResult(
                                    RequestResult.success(200, 10_000_000L, 100, Instant.now()));
                        }
                        for (int j = 0; j < failurePerThread; j++) {
                            collector.recordResult(
                                    RequestResult.failure("error", 5_000_000L, Instant.now()));
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

        LiveMetricsSnapshot snap = collector.snapshot();

        assertEquals(expectedTotal, snap.totalRequests(),
                "Total requests must be exactly " + expectedTotal);
        assertEquals(expectedSuccess, snap.successCount(),
                "Success count must be exactly " + expectedSuccess);
        assertEquals(expectedFailure, snap.failureCount(),
                "Failure count must be exactly " + expectedFailure);
        assertEquals(expectedTotal, snap.successCount() + snap.failureCount(),
                "success + failure must equal total");
    }

    @Test
    @DisplayName("snapshot during concurrent writes does not throw")
    void concurrency_snapshotDuringWrites() throws InterruptedException {
        int writerCount = 100;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(writerCount + 1);

        // Writers
        IntStream.range(0, writerCount).forEach(i ->
                Thread.ofVirtual().start(() -> {
                    try {
                        startGate.await();
                        for (int j = 0; j < 1000; j++) {
                            collector.recordResult(
                                    RequestResult.success(200, 10_000_000L, 100, Instant.now()));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                })
        );

        // Reader (snapshots during writes)
        Thread.ofVirtual().start(() -> {
            try {
                startGate.await();
                for (int j = 0; j < 50; j++) {
                    LiveMetricsSnapshot snap = collector.snapshot();
                    assertNotNull(snap);
                    assertTrue(snap.totalRequests() >= 0);
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        startGate.countDown();
        doneLatch.await();

        // Final snapshot should have all results
        LiveMetricsSnapshot finalSnap = collector.snapshot();
        assertEquals(writerCount * 1000L, finalSnap.totalRequests());
    }

}
