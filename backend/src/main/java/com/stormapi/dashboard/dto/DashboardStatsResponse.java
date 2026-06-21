package com.stormapi.dashboard.dto;

import com.stormapi.test.dto.TestSummaryResponse;
import com.stormapi.test.model.TestType;

import java.util.List;
import java.util.Map;

/**
 * Aggregated dashboard statistics — single API call to populate the dashboard page.
 */
public record DashboardStatsResponse(
        long totalTests,
        long totalRuns,
        long runningTests,
        long completedTests,
        long failedTests,
        long totalRequestsSent,
        double avgResponseTimeMs,
        double avgThroughputRps,
        double avgErrorRate,
        List<TestSummaryResponse> recentTests,
        Map<TestType, Long> testTypeDistribution
) {}
