package com.stormapi.test.dto;

import com.stormapi.common.validation.ValidUrl;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for creating a new test configuration.
 * Jakarta Validation handles field-level rules; TestConfigValidator handles cross-field rules.
 */
public record CreateTestRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @NotBlank @ValidUrl String targetUrl,
        @NotNull HttpMethod httpMethod,
        Map<String, String> headers,
        String requestBody,
        @NotNull TestType testType,
        @Min(1) @Max(10000) int virtualUsers,
        @Min(1) @Max(86400) int durationSeconds,
        @Min(0) int rampUpSeconds,
        Integer stepSize,
        Integer stepDurationSeconds,
        Integer spikeUsers,
        @Min(0) @Max(5) int maxRetries,
        @Min(100) @Max(60000) int timeoutMs,
        @Min(0) @Max(60000) int thinkTimeMs,
        boolean autoStart
) {
    /** Defaults for optional primitives */
    public CreateTestRequest {
        if (timeoutMs == 0) timeoutMs = 5000;
    }
}
