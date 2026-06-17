package com.stormapi.engine.metrics;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable, point-in-time snapshot of all metrics.
 *
 * This is the data contract between the metrics engine and every downstream
 * consumer: WebSocket broadcaster (Phase 9), REST API (Phase 8), persistence
 * layer (Phase 6), and frontend dashboard (Phase 12).
 *
 * Thread-safe: Java record with all immutable fields. Safe to pass across
 * threads without copying. The statusCodeDistribution map is unmodifiable.
 */
public record LiveMetricsSnapshot(
        long totalRequests,
        long successCount,
        long failureCount,
        double avgResponseTimeMs,
        double minResponseTimeMs,
        double maxResponseTimeMs,
        double p50Ms,
        double p75Ms,
        double p90Ms,
        double p95Ms,
        double p99Ms,
        double throughputRps,
        double errorRate,
        int activeUsers,
        long totalDataBytes,
        Map<Integer, Long> statusCodeDistribution,
        Instant timestamp
) {

    /**
     * Compact constructor — ensures statusCodeDistribution is unmodifiable.
     */
    public LiveMetricsSnapshot {
        statusCodeDistribution = (statusCodeDistribution == null || statusCodeDistribution.isEmpty())
                ? Map.of()
                : Map.copyOf(statusCodeDistribution);
    }

}
