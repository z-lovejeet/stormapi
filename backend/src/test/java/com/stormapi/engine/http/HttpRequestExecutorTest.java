package com.stormapi.engine.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for HttpRequestExecutor using WireMock.
 * Verifies all HTTP methods, error handling, timing accuracy, and header forwarding.
 */
class HttpRequestExecutorTest {

    private static WireMockServer wireMock;
    private static HttpRequestExecutor executor;

    @BeforeAll
    static void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        HttpClient client = HttpClientFactory.create(Duration.ofSeconds(2));
        executor = new HttpRequestExecutor(client);
    }

    @AfterAll
    static void tearDown() {
        wireMock.stop();
    }

    private String baseUrl() {
        return "http://localhost:" + wireMock.port();
    }

    // --- Success cases ---

    @Test
    void get_success_200() {
        wireMock.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse().withStatus(200).withBody("Hello")));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/test", "GET", null, null, Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.success()).isTrue();
        assertThat(result.responseBodySize()).isEqualTo(5);
        assertThat(result.errorMessage()).isNull();
        assertThat(result.responseTimeNanos()).isPositive();
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    void post_success_201() {
        wireMock.stubFor(post(urlEqualTo("/create"))
                .willReturn(aResponse().withStatus(201).withBody("Created")));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/create", "POST", null, "{\"name\":\"test\"}",
                Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        assertThat(result.statusCode()).isEqualTo(201);
        assertThat(result.success()).isTrue();
    }

    @Test
    void delete_success_204() {
        wireMock.stubFor(delete(urlEqualTo("/resource/1"))
                .willReturn(aResponse().withStatus(204)));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/resource/1", "DELETE", null, null, Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        assertThat(result.statusCode()).isEqualTo(204);
        assertThat(result.success()).isTrue();
    }

    @Test
    void put_success_200() {
        wireMock.stubFor(put(urlEqualTo("/update"))
                .willReturn(aResponse().withStatus(200).withBody("Updated")));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/update", "PUT", null, "{\"name\":\"updated\"}",
                Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.success()).isTrue();
    }

    // --- Error status codes (still a valid HTTP response, not a connection failure) ---

    @Test
    void serverError_500() {
        wireMock.stubFor(get(urlEqualTo("/error"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/error", "GET", null, null, Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        assertThat(result.statusCode()).isEqualTo(500);
        assertThat(result.success()).isFalse();
        // errorMessage is null because the HTTP request itself succeeded (server responded)
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void notFound_404() {
        wireMock.stubFor(get(urlEqualTo("/missing"))
                .willReturn(aResponse().withStatus(404)));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/missing", "GET", null, null, Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        assertThat(result.statusCode()).isEqualTo(404);
        assertThat(result.success()).isFalse();
    }

    // --- Failure cases ---

    @Test
    void timeout_returnsFailure() {
        wireMock.stubFor(get(urlEqualTo("/slow"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(5000)));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/slow", "GET", null, null, Duration.ofMillis(200));
        RequestResult result = executor.execute(spec);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).containsIgnoringCase("timed out");
        assertThat(result.statusCode()).isZero();
    }

    @Test
    void connectionRefused_returnsFailure() {
        // Use a port that is definitely not listening
        RequestSpec spec = new RequestSpec(
                "http://localhost:19999/nothing", "GET", null, null,
                Duration.ofSeconds(2));
        RequestResult result = executor.execute(spec);

        assertThat(result.success()).isFalse();
        assertThat(result.statusCode()).isZero();
        assertThat(result.errorMessage()).isNotBlank();
    }

    // --- Latency measurement ---

    @Test
    void latencyMeasurement_isAccurate() {
        wireMock.stubFor(get(urlEqualTo("/delayed"))
                .willReturn(aResponse().withStatus(200).withBody("ok")
                        .withFixedDelay(100)));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/delayed", "GET", null, null, Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        // 100ms delay → responseTime should be ~100ms (100_000_000 nanos) ± tolerance
        double responseMs = result.responseTimeMs();
        assertThat(responseMs).isBetween(80.0, 500.0);
    }

    // --- Header forwarding ---

    @Test
    void headersAreSent() {
        wireMock.stubFor(get(urlEqualTo("/headers"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .willReturn(aResponse().withStatus(200)));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/headers", "GET",
                Map.of("Authorization", "Bearer test-token"),
                null, Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.success()).isTrue();
    }

    // --- Request body forwarding ---

    @Test
    void requestBodyIsSent() {
        String requestBody = "{\"key\":\"value\"}";
        wireMock.stubFor(post(urlEqualTo("/body"))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse().withStatus(200)));

        RequestSpec spec = new RequestSpec(
                baseUrl() + "/body", "POST",
                Map.of("Content-Type", "application/json"),
                requestBody, Duration.ofSeconds(5));
        RequestResult result = executor.execute(spec);

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.success()).isTrue();
    }

}
