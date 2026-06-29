package com.stormapi.test.dto;

import java.util.List;

/**
 * Response DTO for side-by-side test result comparison.
 */
public record ComparisonResponse(
        TestResultResponse resultA,
        TestResultResponse resultB,
        List<MetricDelta> deltas
) {}
