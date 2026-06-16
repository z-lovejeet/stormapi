package com.stormapi.engine.http;

import com.stormapi.test.model.TestConfig;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable specification for a single HTTP request.
 * Decouples the test configuration domain model from the HTTP execution layer.
 *
 * Created once per test run and shared (read-only) across all virtual users.
 */
public record RequestSpec(
        String url,
        String method,
        Map<String, String> headers,
        String body,
        Duration timeout
) {

    /**
     * Compact constructor — validates invariants and normalizes headers.
     */
    public RequestSpec {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be null or blank");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be a positive duration");
        }
        // Normalize headers to unmodifiable map
        headers = (headers == null || headers.isEmpty()) ? Map.of() : Map.copyOf(headers);
    }

    /**
     * Factory method to create a RequestSpec from a persisted TestConfig entity.
     */
    public static RequestSpec fromTestConfig(TestConfig config) {
        Objects.requireNonNull(config, "TestConfig must not be null");

        return new RequestSpec(
                config.getTargetUrl(),
                config.getHttpMethod().name(),
                config.getHeaders(),
                config.getRequestBody(),
                Duration.ofMillis(config.getTimeoutMs())
        );
    }

}
