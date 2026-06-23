package com.stormapi.websocket.dto;

import java.util.Map;

/**
 * WebSocket DTO for live metrics broadcasting.
 * Sent to /topic/metrics/{testId} every ~1 second during test execution.
 *
 * Mirrors {@link com.stormapi.engine.metrics.LiveMetricsSnapshot} with
 * testId added for client-side routing and String timestamp for JSON.
 */
public record LiveMetricsMessage(
        long testId,
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
        String timestamp
) {

    /**
     * Creates a LiveMetricsMessage from a LiveMetricsSnapshot.
     */
    public static LiveMetricsMessage from(long testId,
                                           com.stormapi.engine.metrics.LiveMetricsSnapshot snapshot) {
        return new LiveMetricsMessage(
                testId,
                snapshot.totalRequests(),
                snapshot.successCount(),
                snapshot.failureCount(),
                snapshot.avgResponseTimeMs(),
                snapshot.minResponseTimeMs(),
                snapshot.maxResponseTimeMs(),
                snapshot.p50Ms(),
                snapshot.p75Ms(),
                snapshot.p90Ms(),
                snapshot.p95Ms(),
                snapshot.p99Ms(),
                snapshot.throughputRps(),
                snapshot.errorRate(),
                snapshot.activeUsers(),
                snapshot.totalDataBytes(),
                snapshot.statusCodeDistribution(),
                snapshot.timestamp().toString()
        );
    }

}
