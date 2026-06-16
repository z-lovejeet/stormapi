package com.stormapi.engine.http;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Creates properly configured {@link HttpClient} instances per test run.
 *
 * Each test run gets its own HttpClient to prevent connection pool
 * contamination between tests. The client uses virtual thread executor
 * for non-blocking async operations.
 */
public final class HttpClientFactory {

    private HttpClientFactory() {
        // utility class — prevent instantiation
    }

    /**
     * Creates an HttpClient with the specified connect timeout.
     *
     * Configuration:
     * - HTTP/1.1 (avoids H2 multiplexing head-of-line blocking)
     * - No redirect following (we want to see 3xx status codes)
     * - Virtual thread executor for async operations
     */
    public static HttpClient create(Duration connectTimeout) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(connectTimeout)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    /**
     * Creates an HttpClient with the default 5-second connect timeout.
     */
    public static HttpClient createDefault() {
        return create(Duration.ofSeconds(5));
    }

}
