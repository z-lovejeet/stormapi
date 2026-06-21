package com.stormapi.test.dto;

import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;

import java.time.Instant;
import java.util.Map;

/**
 * Full test config response — used for GET /api/tests/{id}.
 */
public record TestConfigResponse(
        Long id,
        String name,
        String description,
        String targetUrl,
        HttpMethod httpMethod,
        Map<String, String> headers,
        String requestBody,
        TestType testType,
        int virtualUsers,
        int durationSeconds,
        int rampUpSeconds,
        Integer stepSize,
        Integer stepDurationSeconds,
        Integer spikeUsers,
        int maxRetries,
        int timeoutMs,
        int thinkTimeMs,
        TestStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
