package com.stormapi.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApiResponse serialization and factory methods.
 */
class ApiResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void successResponse_containsDataAndTimestamp() throws Exception {
        ApiResponse<String> response = ApiResponse.success("hello", "/api/test");

        String json = objectMapper.writeValueAsString(response);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getError()).isNull();
        assertThat(response.getPath()).isEqualTo("/api/test");
        assertThat(response.getTimestamp()).isNotNull();

        // Verify JSON structure
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"data\":\"hello\"");
        assertThat(json).contains("\"path\":\"/api/test\"");
        assertThat(json).doesNotContain("\"error\""); // null fields excluded
    }

    @Test
    void errorResponse_containsErrorDetails() throws Exception {
        ApiResponse<Void> response = ApiResponse.error(
                404, "Not Found", "TestConfig with id 42 not found",
                "RESOURCE_NOT_FOUND", "/api/tests/42"
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getStatus()).isEqualTo(404);
        assertThat(response.getError().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");

        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"errorCode\":\"RESOURCE_NOT_FOUND\"");
        assertThat(json).doesNotContain("\"data\""); // null fields excluded
    }

    @Test
    void validationErrorResponse_containsFieldErrors() throws Exception {
        Map<String, String> fieldErrors = Map.of(
                "targetUrl", "Invalid URL format",
                "virtualUsers", "must be greater than 0"
        );

        ApiResponse<Void> response = ApiResponse.validationError(
                "Validation failed", fieldErrors, "/api/tests"
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(response.getError().getFieldErrors()).hasSize(2);
        assertThat(response.getError().getFieldErrors()).containsKey("targetUrl");

        assertThat(json).contains("\"fieldErrors\"");
        assertThat(json).contains("\"targetUrl\"");
    }

    @Test
    void timestampIsIso8601Format() throws Exception {
        ApiResponse<String> response = ApiResponse.success("data", "/test");

        String json = objectMapper.writeValueAsString(response);

        // ISO-8601 format contains 'T' separator and 'Z' suffix
        assertThat(json).containsPattern("\\d{4}-\\d{2}-\\d{2}T");
    }

}
