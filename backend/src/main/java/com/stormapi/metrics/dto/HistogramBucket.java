package com.stormapi.metrics.dto;

/**
 * A single histogram bucket for response time distribution.
 */
public record HistogramBucket(
        String range,
        long count
) {}
