package com.stormapi.test.dto;

/**
 * Represents the delta between two metric values for comparison.
 */
public record MetricDelta(
        String field,
        String label,
        double resultA,
        double resultB,
        double delta,
        double deltaPercent,
        boolean improved
) {}
