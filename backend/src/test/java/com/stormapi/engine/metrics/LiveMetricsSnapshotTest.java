package com.stormapi.engine.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LiveMetricsSnapshot Tests")
class LiveMetricsSnapshotTest {

    @Test
    @DisplayName("all fields are accessible with correct values")
    void allFieldsAccessible() {
        Map<Integer, Long> statusCodes = Map.of(200, 90L, 500, 10L);
        Instant now = Instant.now();

        LiveMetricsSnapshot snap = new LiveMetricsSnapshot(
                100, 90, 10,
                50.5, 5.0, 500.0,
                45.0, 60.0, 80.0, 95.0, 120.0,
                1000.0, 10.0, 50,
                1024L, statusCodes, now
        );

        assertEquals(100, snap.totalRequests());
        assertEquals(90, snap.successCount());
        assertEquals(10, snap.failureCount());
        assertEquals(50.5, snap.avgResponseTimeMs());
        assertEquals(5.0, snap.minResponseTimeMs());
        assertEquals(500.0, snap.maxResponseTimeMs());
        assertEquals(45.0, snap.p50Ms());
        assertEquals(60.0, snap.p75Ms());
        assertEquals(80.0, snap.p90Ms());
        assertEquals(95.0, snap.p95Ms());
        assertEquals(120.0, snap.p99Ms());
        assertEquals(1000.0, snap.throughputRps());
        assertEquals(10.0, snap.errorRate());
        assertEquals(50, snap.activeUsers());
        assertEquals(1024L, snap.totalDataBytes());
        assertEquals(now, snap.timestamp());
    }

    @Test
    @DisplayName("statusCodeDistribution is unmodifiable")
    void statusCodeDistribution_isUnmodifiable() {
        Map<Integer, Long> mutableMap = new java.util.HashMap<>();
        mutableMap.put(200, 100L);
        mutableMap.put(404, 5L);

        LiveMetricsSnapshot snap = new LiveMetricsSnapshot(
                105, 100, 5,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                mutableMap, Instant.now()
        );

        assertThrows(UnsupportedOperationException.class,
                () -> snap.statusCodeDistribution().put(500, 1L));
    }

    @Test
    @DisplayName("null statusCodeDistribution becomes empty map")
    void nullStatusCodeDistribution_becomesEmptyMap() {
        LiveMetricsSnapshot snap = new LiveMetricsSnapshot(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                null, Instant.now()
        );

        assertNotNull(snap.statusCodeDistribution());
        assertTrue(snap.statusCodeDistribution().isEmpty());
    }

}
