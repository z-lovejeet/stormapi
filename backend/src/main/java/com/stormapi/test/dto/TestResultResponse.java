package com.stormapi.test.dto;

import com.stormapi.test.model.TestStatus;

import java.time.Instant;

/**
 * Response DTO for test execution results.
 * Includes all aggregate metrics, latency percentiles, and engine-specific analysis fields.
 */
public record TestResultResponse(
        Long id,
        Long testConfigId,
        TestStatus status,
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
        long totalDataBytes,
        Instant startedAt,
        Instant completedAt,
        long durationMs,
        Integer breakpointUsers,
        Long recoveryTimeMs,
        Double degradationSlope,
        Boolean degradationDetected,
        Instant createdAt
) {}
