package com.stormapi.engine.assertion;

import java.util.Map;

/**
 * Immutable context holding all HTTP response data needed by assertions.
 *
 * <p>Built from {@code DetailedRequestResult} after each HTTP request.
 * Passed to every {@link Assertion#evaluate} call.
 *
 * @param statusCode      HTTP status code (0 if request failed)
 * @param responseTimeMs  response latency in milliseconds
 * @param responseBody    response body text (may be null if request failed)
 * @param responseHeaders response headers as key-value map (may be null)
 */
public record AssertionContext(
        int statusCode,
        double responseTimeMs,
        String responseBody,
        Map<String, String> responseHeaders
) {
}
