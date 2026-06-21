package com.stormapi.metrics.dto;

import java.time.Instant;

/**
 * Response DTO for individual request log entries.
 */
public record RequestLogResponse(
        Long id,
        Instant timestamp,
        String url,
        String method,
        int statusCode,
        long responseTimeMs,
        long responseSize,
        String errorMessage,
        boolean success
) {}
