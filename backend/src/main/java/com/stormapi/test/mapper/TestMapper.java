package com.stormapi.test.mapper;

import com.stormapi.test.dto.CreateTestRequest;
import com.stormapi.test.dto.TestConfigResponse;
import com.stormapi.test.dto.TestResultResponse;
import com.stormapi.test.dto.TestSummaryResponse;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;

/**
 * Manual mapper between TestConfig/TestResult entities and their DTOs.
 * Stateless utility class — no Spring bean, no dependency injection.
 */
public final class TestMapper {

    private TestMapper() {} // Prevent instantiation

    /**
     * Maps CreateTestRequest DTO to TestConfig entity.
     * Sets initial status to CREATED.
     */
    public static TestConfig toEntity(CreateTestRequest request) {
        return TestConfig.builder()
                .name(request.name())
                .description(request.description())
                .targetUrl(request.targetUrl())
                .httpMethod(request.httpMethod())
                .headers(request.headers())
                .requestBody(request.requestBody())
                .testType(request.testType())
                .virtualUsers(request.virtualUsers())
                .durationSeconds(request.durationSeconds())
                .rampUpSeconds(request.rampUpSeconds())
                .stepSize(request.stepSize())
                .stepDurationSeconds(request.stepDurationSeconds())
                .spikeUsers(request.spikeUsers())
                .maxRetries(request.maxRetries())
                .timeoutMs(request.timeoutMs())
                .thinkTimeMs(request.thinkTimeMs())
                .status(TestStatus.CREATED)
                .build();
    }

    /**
     * Maps TestConfig entity to full response DTO.
     */
    public static TestConfigResponse toResponse(TestConfig config) {
        return new TestConfigResponse(
                config.getId(),
                config.getName(),
                config.getDescription(),
                config.getTargetUrl(),
                config.getHttpMethod(),
                config.getHeaders(),
                config.getRequestBody(),
                config.getTestType(),
                config.getVirtualUsers(),
                config.getDurationSeconds(),
                config.getRampUpSeconds(),
                config.getStepSize(),
                config.getStepDurationSeconds(),
                config.getSpikeUsers(),
                config.getMaxRetries(),
                config.getTimeoutMs(),
                config.getThinkTimeMs(),
                config.getStatus(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    /**
     * Maps TestConfig entity + optional latest TestResult to lightweight summary DTO.
     *
     * @param config the test configuration
     * @param latestResult the most recent result, may be null
     */
    public static TestSummaryResponse toSummary(TestConfig config, TestResult latestResult) {
        return new TestSummaryResponse(
                config.getId(),
                config.getName(),
                config.getTargetUrl(),
                config.getTestType(),
                config.getStatus(),
                config.getVirtualUsers(),
                config.getDurationSeconds(),
                latestResult != null ? latestResult.getStartedAt() : null,
                latestResult != null ? latestResult.getAvgResponseTimeMs() : null,
                latestResult != null ? latestResult.getErrorRate() : null,
                config.getResults() != null ? config.getResults().size() : 0,
                config.getCreatedAt()
        );
    }

    /**
     * Maps TestResult entity to response DTO.
     */
    public static TestResultResponse toResultResponse(TestResult result) {
        return new TestResultResponse(
                result.getId(),
                result.getTestConfig() != null ? result.getTestConfig().getId() : null,
                result.getStatus(),
                result.getTotalRequests(),
                result.getSuccessCount(),
                result.getFailureCount(),
                result.getAvgResponseTimeMs(),
                result.getMinResponseTimeMs(),
                result.getMaxResponseTimeMs(),
                result.getP50Ms(),
                result.getP75Ms(),
                result.getP90Ms(),
                result.getP95Ms(),
                result.getP99Ms(),
                result.getThroughputRps(),
                result.getErrorRate(),
                result.getTotalDataBytes(),
                result.getStartedAt(),
                result.getCompletedAt(),
                result.getDurationMs(),
                result.getBreakpointUsers(),
                result.getRecoveryTimeMs(),
                result.getDegradationSlope(),
                result.getDegradationDetected(),
                result.getCreatedAt()
        );
    }

}
