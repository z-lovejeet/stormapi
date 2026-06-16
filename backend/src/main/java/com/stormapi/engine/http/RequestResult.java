package com.stormapi.engine.http;

import java.time.Instant;

/**
 * Immutable result of a single HTTP request execution.
 * Carries status code, nanoTime-based latency, response size, and error info.
 *
 * Errors are encoded as values — this record is NEVER wrapped in an exception.
 * Short-lived: created per request, consumed by MetricsCollector, then GC'd.
 */
public record RequestResult(
        int statusCode,
        long responseTimeNanos,
        long responseBodySize,
        boolean success,
        String errorMessage,
        Instant timestamp
) {

    /**
     * Factory for successful requests (2xx status codes).
     */
    public static RequestResult success(int statusCode, long responseTimeNanos,
                                        long responseBodySize, Instant timestamp) {
        return new RequestResult(statusCode, responseTimeNanos, responseBodySize,
                statusCode >= 200 && statusCode <= 299, null, timestamp);
    }

    /**
     * Factory for failed requests (connection errors, timeouts, etc.).
     */
    public static RequestResult failure(String errorMessage, long responseTimeNanos,
                                        Instant timestamp) {
        return new RequestResult(0, responseTimeNanos, 0, false, errorMessage, timestamp);
    }

    /**
     * Convenience: converts nanoTime to milliseconds for display and metrics.
     */
    public double responseTimeMs() {
        return responseTimeNanos / 1_000_000.0;
    }

}
