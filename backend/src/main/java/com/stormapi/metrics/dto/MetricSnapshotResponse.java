package com.stormapi.metrics.dto;

import java.time.Instant;

/**
 * Response DTO for time-series metric snapshots.
 * Excludes id and FK — frontend only needs chart data.
 */
public record MetricSnapshotResponse(
        Instant timestamp,
        int activeUsers,
        double requestsPerSecond,
        double avgResponseTimeMs,
        double errorRate,
        double p95Ms,
        long cumulativeRequests,
        long cumulativeErrors
) {}
