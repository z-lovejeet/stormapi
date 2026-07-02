package com.stormapi.engine.http;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Core request execution unit — sends HTTP requests and measures latency.
 *
 * NEVER throws exceptions to callers. All errors are encoded as
 * {@link RequestResult#failure} values. This prevents virtual user
 * threads from crashing during load tests.
 *
 * Uses {@link System#nanoTime()} for precise, monotonic latency measurement
 * (immune to NTP clock adjustments). This matches what JMeter and Gatling use.
 */
public class HttpRequestExecutor {

    private final HttpClient httpClient;

    public HttpRequestExecutor(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Executes an HTTP request synchronously and returns the result.
     *
     * Synchronous execution is preferred over async when running on virtual
     * threads — the virtual thread unmounts during I/O, so blocking is "free"
     * with simpler code and better stack traces.
     *
     * @param spec the request specification
     * @return result with status code, latency, size, and error info
     */
    public RequestResult execute(RequestSpec spec) {
        long startNanos = System.nanoTime();
        Instant timestamp = Instant.now();

        try {
            HttpRequest request = buildRequest(spec);
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            long elapsedNanos = System.nanoTime() - startNanos;
            String body = response.body();
            long bodySize = (body != null) ? body.length() : 0;

            return RequestResult.success(response.statusCode(), elapsedNanos,
                    bodySize, timestamp);

        } catch (HttpTimeoutException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return RequestResult.failure(
                    "Request timed out after " + spec.timeout(), elapsedNanos, timestamp);

        } catch (ConnectException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return RequestResult.failure(
                    "Connection refused: " + spec.url(), elapsedNanos, timestamp);

        } catch (UnresolvedAddressException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return RequestResult.failure(
                    "DNS resolution failed: " + spec.url(), elapsedNanos, timestamp);

        } catch (SSLException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return RequestResult.failure(
                    "SSL/TLS error: " + e.getMessage(), elapsedNanos, timestamp);

        } catch (InterruptedException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            Thread.currentThread().interrupt(); // restore interrupt flag
            return RequestResult.failure(
                    "Execution interrupted", elapsedNanos, timestamp);

        } catch (IOException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return RequestResult.failure(
                    "I/O error: " + e.getMessage(), elapsedNanos, timestamp);
        }
    }

    /**
     * Executes an HTTP request asynchronously.
     * Wraps the sync execute() in a CompletableFuture for callers
     * that need Future-based composition.
     */
    public CompletableFuture<RequestResult> executeAsync(RequestSpec spec) {
        return CompletableFuture.supplyAsync(() -> execute(spec));
    }

    /**
     * Executes an HTTP request synchronously and returns the result
     * <b>including the response body</b>.
     *
     * <p>Used by the scenario execution engine where the response body
     * is needed for variable extraction. The standard {@link #execute}
     * method discards the body to minimize memory usage during load tests.</p>
     *
     * @param spec the request specification
     * @return detailed result including response body text
     */
    public DetailedRequestResult executeWithBody(RequestSpec spec) {
        long startNanos = System.nanoTime();
        Instant timestamp = Instant.now();

        try {
            HttpRequest request = buildRequest(spec);
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            long elapsedNanos = System.nanoTime() - startNanos;
            String body = response.body();
            long bodySize = (body != null) ? body.length() : 0;
            boolean isSuccess = response.statusCode() >= 200 && response.statusCode() <= 299;

            // Capture response headers for assertion evaluation
            Map<String, String> headers = new LinkedHashMap<>();
            response.headers().map().forEach((name, values) -> {
                if (!values.isEmpty()) {
                    headers.put(name, values.getFirst());
                }
            });

            return new DetailedRequestResult(
                    response.statusCode(), elapsedNanos, bodySize,
                    isSuccess, null, timestamp, body, headers);

        } catch (HttpTimeoutException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return new DetailedRequestResult(0, elapsedNanos, 0, false,
                    "Request timed out after " + spec.timeout(), timestamp, null, null);

        } catch (ConnectException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return new DetailedRequestResult(0, elapsedNanos, 0, false,
                    "Connection refused: " + spec.url(), timestamp, null, null);

        } catch (UnresolvedAddressException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return new DetailedRequestResult(0, elapsedNanos, 0, false,
                    "DNS resolution failed: " + spec.url(), timestamp, null, null);

        } catch (SSLException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return new DetailedRequestResult(0, elapsedNanos, 0, false,
                    "SSL/TLS error: " + e.getMessage(), timestamp, null, null);

        } catch (InterruptedException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            Thread.currentThread().interrupt();
            return new DetailedRequestResult(0, elapsedNanos, 0, false,
                    "Execution interrupted", timestamp, null, null);

        } catch (IOException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return new DetailedRequestResult(0, elapsedNanos, 0, false,
                    "I/O error: " + e.getMessage(), timestamp, null, null);
        }
    }

    /**
     * Extended result that includes the response body text and headers.
     * Used by scenario execution for variable extraction and assertion evaluation.
     */
    public record DetailedRequestResult(
            int statusCode,
            long responseTimeNanos,
            long responseBodySize,
            boolean success,
            String errorMessage,
            Instant timestamp,
            String responseBody,
            Map<String, String> responseHeaders
    ) {
        /** Convenience: converts nanoTime to milliseconds. */
        public double responseTimeMs() {
            return responseTimeNanos / 1_000_000.0;
        }
    }

    /**
     * Builds a java.net.http.HttpRequest from a RequestSpec.
     */
    private HttpRequest buildRequest(RequestSpec spec) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(spec.url()))
                .timeout(spec.timeout());

        // Apply headers
        if (spec.headers() != null && !spec.headers().isEmpty()) {
            for (Map.Entry<String, String> entry : spec.headers().entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        // Apply method with appropriate body publisher
        HttpRequest.BodyPublisher bodyPublisher = (spec.body() != null && !spec.body().isEmpty())
                ? HttpRequest.BodyPublishers.ofString(spec.body())
                : HttpRequest.BodyPublishers.noBody();

        builder.method(spec.method(), bodyPublisher);

        return builder.build();
    }

}
