package com.stormapi.engine.http;

import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestSpecTest {

    @Test
    void fromTestConfig_mapsAllFieldsCorrectly() {
        TestConfig config = TestConfig.builder()
                .name("Test")
                .targetUrl("https://api.example.com/users")
                .httpMethod(HttpMethod.POST)
                .headers(Map.of("Authorization", "Bearer token123"))
                .requestBody("{\"name\":\"test\"}")
                .testType(TestType.LOAD)
                .virtualUsers(10)
                .durationSeconds(60)
                .timeoutMs(3000)
                .build();

        RequestSpec spec = RequestSpec.fromTestConfig(config);

        assertThat(spec.url()).isEqualTo("https://api.example.com/users");
        assertThat(spec.method()).isEqualTo("POST");
        assertThat(spec.headers()).containsEntry("Authorization", "Bearer token123");
        assertThat(spec.body()).isEqualTo("{\"name\":\"test\"}");
        assertThat(spec.timeout()).isEqualTo(Duration.ofMillis(3000));
    }

    @Test
    void nullHeaders_normalizedToEmptyMap() {
        RequestSpec spec = new RequestSpec(
                "https://example.com", "GET", null, null, Duration.ofSeconds(5));

        assertThat(spec.headers()).isEmpty();
        assertThat(spec.headers()).isNotNull();
    }

    @Test
    void blankUrl_throwsException() {
        assertThatThrownBy(() ->
                new RequestSpec("", "GET", null, null, Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }

    @Test
    void negativeTimeout_throwsException() {
        assertThatThrownBy(() ->
                new RequestSpec("https://example.com", "GET", null, null,
                        Duration.ofMillis(-100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timeout");
    }

    @Test
    void immutability_headersCannotBeModified() {
        Map<String, String> mutableHeaders = new HashMap<>();
        mutableHeaders.put("X-Test", "value");

        RequestSpec spec = new RequestSpec(
                "https://example.com", "GET", mutableHeaders, null, Duration.ofSeconds(5));

        assertThatThrownBy(() -> spec.headers().put("X-New", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
