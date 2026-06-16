package com.stormapi.engine.http;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResultTest {

    @Test
    void successFactory_setsCorrectFields() {
        Instant now = Instant.now();
        RequestResult result = RequestResult.success(200, 5_000_000L, 1024, now);

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.responseTimeNanos()).isEqualTo(5_000_000L);
        assertThat(result.responseBodySize()).isEqualTo(1024);
        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.timestamp()).isEqualTo(now);
    }

    @Test
    void failureFactory_setsCorrectFields() {
        Instant now = Instant.now();
        RequestResult result = RequestResult.failure("Connection refused", 1_000_000L, now);

        assertThat(result.statusCode()).isZero();
        assertThat(result.responseTimeNanos()).isEqualTo(1_000_000L);
        assertThat(result.responseBodySize()).isZero();
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Connection refused");
        assertThat(result.timestamp()).isEqualTo(now);
    }

    @Test
    void responseTimeMs_convertsCorrectly() {
        Instant now = Instant.now();
        RequestResult result = RequestResult.success(200, 5_000_000L, 100, now);

        assertThat(result.responseTimeMs()).isEqualTo(5.0);
    }

    @Test
    void successDetermination_2xxOnly() {
        Instant now = Instant.now();

        // 2xx = success
        assertThat(RequestResult.success(200, 1000, 0, now).success()).isTrue();
        assertThat(RequestResult.success(201, 1000, 0, now).success()).isTrue();
        assertThat(RequestResult.success(299, 1000, 0, now).success()).isTrue();

        // 3xx, 4xx, 5xx = not success
        assertThat(RequestResult.success(301, 1000, 0, now).success()).isFalse();
        assertThat(RequestResult.success(404, 1000, 0, now).success()).isFalse();
        assertThat(RequestResult.success(500, 1000, 0, now).success()).isFalse();
    }

}
