package com.stormapi.websocket.dto;

import com.stormapi.engine.http.RequestResult;

import java.util.List;

/**
 * WebSocket DTO for batched request log broadcasting.
 * Sent to /topic/logs/{testId} every ~1 second during test execution.
 *
 * Contains a batch of up to 50 recent request log entries.
 * This is a sampled stream — complete request logs are available via REST API.
 */
public record RequestLogMessage(
        long testId,
        List<RequestLogEntry> entries
) {

    /**
     * Individual request log entry within a batch.
     */
    public record RequestLogEntry(
            String timestamp,
            String url,
            String method,
            int statusCode,
            double responseTimeMs,
            long responseSize,
            String errorMessage,
            boolean success
    ) {

        /**
         * Creates a RequestLogEntry from a RequestResult and request metadata.
         */
        public static RequestLogEntry from(RequestResult result, String url, String method) {
            return new RequestLogEntry(
                    result.timestamp().toString(),
                    url,
                    method,
                    result.statusCode(),
                    result.responseTimeMs(),
                    result.responseBodySize(),
                    result.errorMessage(),
                    result.success()
            );
        }

    }

}
