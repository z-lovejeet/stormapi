package com.stormapi.test.dto;

import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;

import java.time.Instant;

/**
 * Lightweight response DTO for paginated test list views.
 * Enriched with latest result metrics to avoid separate API calls.
 */
public record TestSummaryResponse(
        Long id,
        String name,
        String targetUrl,
        TestType testType,
        TestStatus status,
        int virtualUsers,
        int durationSeconds,
        Instant lastRunAt,
        Double lastAvgResponseTimeMs,
        Double lastErrorRate,
        int totalRuns,
        Instant createdAt
) {}
