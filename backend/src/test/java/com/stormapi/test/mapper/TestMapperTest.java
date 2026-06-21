package com.stormapi.test.mapper;

import com.stormapi.test.dto.CreateTestRequest;
import com.stormapi.test.dto.TestConfigResponse;
import com.stormapi.test.dto.TestResultResponse;
import com.stormapi.test.dto.TestSummaryResponse;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestMapper Unit Tests")
class TestMapperTest {

    @Test
    @DisplayName("toEntity maps all fields from CreateTestRequest")
    void toEntity_mapsAllFields() {
        CreateTestRequest request = new CreateTestRequest(
                "Load Test", "desc", "https://api.example.com", HttpMethod.GET,
                Map.of("Auth", "Bearer token"), null, TestType.LOAD,
                100, 60, 10, null, null, null, 2, 5000, 100, false
        );

        TestConfig config = TestMapper.toEntity(request);

        assertEquals("Load Test", config.getName());
        assertEquals("desc", config.getDescription());
        assertEquals("https://api.example.com", config.getTargetUrl());
        assertEquals(HttpMethod.GET, config.getHttpMethod());
        assertEquals(TestType.LOAD, config.getTestType());
        assertEquals(100, config.getVirtualUsers());
        assertEquals(60, config.getDurationSeconds());
        assertEquals(10, config.getRampUpSeconds());
        assertEquals(2, config.getMaxRetries());
        assertEquals(5000, config.getTimeoutMs());
        assertEquals(100, config.getThinkTimeMs());
        assertEquals(TestStatus.CREATED, config.getStatus());
    }

    @Test
    @DisplayName("toEntity sets default CREATED status")
    void toEntity_setsDefaultStatus() {
        CreateTestRequest request = new CreateTestRequest(
                "Test", null, "https://api.example.com", HttpMethod.GET,
                null, null, TestType.LOAD, 10, 30, 0,
                null, null, null, 0, 5000, 0, true
        );

        TestConfig config = TestMapper.toEntity(request);
        assertEquals(TestStatus.CREATED, config.getStatus());
    }

    @Test
    @DisplayName("toResponse maps all fields from TestConfig")
    void toResponse_mapsAllFields() {
        TestConfig config = buildTestConfig();

        TestConfigResponse response = TestMapper.toResponse(config);

        assertEquals(config.getId(), response.id());
        assertEquals(config.getName(), response.name());
        assertEquals(config.getTargetUrl(), response.targetUrl());
        assertEquals(config.getHttpMethod(), response.httpMethod());
        assertEquals(config.getTestType(), response.testType());
        assertEquals(config.getVirtualUsers(), response.virtualUsers());
        assertEquals(config.getDurationSeconds(), response.durationSeconds());
        assertEquals(config.getStatus(), response.status());
    }

    @Test
    @DisplayName("toSummary with result includes metrics")
    void toSummary_withResult_includesMetrics() {
        TestConfig config = buildTestConfig();
        TestResult result = buildTestResult(config);

        TestSummaryResponse summary = TestMapper.toSummary(config, result);

        assertEquals(config.getId(), summary.id());
        assertEquals(config.getName(), summary.name());
        assertNotNull(summary.lastRunAt());
        assertEquals(result.getAvgResponseTimeMs(), summary.lastAvgResponseTimeMs());
        assertEquals(result.getErrorRate(), summary.lastErrorRate());
    }

    @Test
    @DisplayName("toSummary without result has null metrics")
    void toSummary_withoutResult_nullSafeMetrics() {
        TestConfig config = buildTestConfig();

        TestSummaryResponse summary = TestMapper.toSummary(config, null);

        assertEquals(config.getId(), summary.id());
        assertNull(summary.lastRunAt());
        assertNull(summary.lastAvgResponseTimeMs());
        assertNull(summary.lastErrorRate());
    }

    @Test
    @DisplayName("toResultResponse maps engine-specific fields")
    void toResultResponse_mapsEngineFields() {
        TestConfig config = buildTestConfig();
        TestResult result = buildTestResult(config);
        result.setBreakpointUsers(150);
        result.setRecoveryTimeMs(3000L);
        result.setDegradationSlope(0.5);
        result.setDegradationDetected(true);

        TestResultResponse response = TestMapper.toResultResponse(result);

        assertEquals(150, response.breakpointUsers());
        assertEquals(3000L, response.recoveryTimeMs());
        assertEquals(0.5, response.degradationSlope());
        assertTrue(response.degradationDetected());
        assertEquals(result.getTotalRequests(), response.totalRequests());
        assertEquals(result.getP95Ms(), response.p95Ms());
    }

    // ── Helpers ──

    private TestConfig buildTestConfig() {
        TestConfig config = TestConfig.builder()
                .name("Test Config")
                .targetUrl("https://api.example.com")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.LOAD)
                .virtualUsers(50)
                .durationSeconds(60)
                .rampUpSeconds(10)
                .status(TestStatus.COMPLETED)
                .results(new ArrayList<>())
                .build();
        config.setId(1L);
        config.setCreatedAt(Instant.now());
        config.setUpdatedAt(Instant.now());
        return config;
    }

    private TestResult buildTestResult(TestConfig config) {
        TestResult result = TestResult.builder()
                .testConfig(config)
                .status(TestStatus.COMPLETED)
                .totalRequests(1000)
                .successCount(950)
                .failureCount(50)
                .avgResponseTimeMs(45.5)
                .minResponseTimeMs(5.0)
                .maxResponseTimeMs(500.0)
                .p50Ms(30.0)
                .p75Ms(50.0)
                .p90Ms(80.0)
                .p95Ms(120.0)
                .p99Ms(250.0)
                .throughputRps(16.7)
                .errorRate(5.0)
                .totalDataBytes(500000)
                .startedAt(Instant.now().minusSeconds(60))
                .completedAt(Instant.now())
                .durationMs(60000)
                .build();
        result.setId(10L);
        result.setCreatedAt(Instant.now());
        return result;
    }

}
