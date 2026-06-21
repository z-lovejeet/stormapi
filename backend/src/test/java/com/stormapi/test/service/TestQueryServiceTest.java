package com.stormapi.test.service;

import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.test.dto.TestSummaryResponse;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import com.stormapi.test.repository.TestConfigRepository;
import com.stormapi.test.repository.TestResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestQueryService Unit Tests")
class TestQueryServiceTest {

    @Mock
    private TestConfigRepository testConfigRepository;

    @Mock
    private TestResultRepository testResultRepository;

    @InjectMocks
    private TestQueryService queryService;

    @Test
    @DisplayName("getTestConfig returns config when exists")
    void getTestConfig_exists_returnsConfig() {
        TestConfig config = buildConfig(1L);
        when(testConfigRepository.findById(1L)).thenReturn(Optional.of(config));

        TestConfig result = queryService.getTestConfig(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getTestConfig throws when not found")
    void getTestConfig_notFound_throws() {
        when(testConfigRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> queryService.getTestConfig(99L));
    }

    @Test
    @DisplayName("listTests with no filters returns all")
    void listTests_noFilters_returnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TestConfig> page = new PageImpl<>(List.of(buildConfig(1L)));
        when(testConfigRepository.findAll(pageable)).thenReturn(page);

        Page<TestConfig> result = queryService.listTests(null, null, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("listTests with status filter")
    void listTests_statusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TestConfig> page = new PageImpl<>(List.of(buildConfig(1L)));
        when(testConfigRepository.findByStatus(TestStatus.COMPLETED, pageable)).thenReturn(page);

        Page<TestConfig> result = queryService.listTests(TestStatus.COMPLETED, null, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("listTests with type filter")
    void listTests_typeFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TestConfig> page = new PageImpl<>(List.of(buildConfig(1L)));
        when(testConfigRepository.findByTestType(TestType.LOAD, pageable)).thenReturn(page);

        Page<TestConfig> result = queryService.listTests(null, TestType.LOAD, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("listTests with both filters")
    void listTests_bothFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TestConfig> page = new PageImpl<>(List.of(buildConfig(1L)));
        when(testConfigRepository.findByStatusAndTestType(TestStatus.COMPLETED, TestType.LOAD, pageable))
                .thenReturn(page);

        Page<TestConfig> result = queryService.listTests(TestStatus.COMPLETED, TestType.LOAD, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getLatestResult returns result when exists")
    void getLatestResult_exists() {
        when(testConfigRepository.existsById(1L)).thenReturn(true);
        TestResult testResult = buildResult(10L);
        when(testResultRepository.findTopByTestConfigIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(testResult));

        TestResult result = queryService.getLatestResult(1L);
        assertEquals(10L, result.getId());
    }

    @Test
    @DisplayName("getLatestResult throws when config not found")
    void getLatestResult_configNotFound_throws() {
        when(testConfigRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> queryService.getLatestResult(99L));
    }

    @Test
    @DisplayName("getLatestResult throws when no results")
    void getLatestResult_noResults_throws() {
        when(testConfigRepository.existsById(1L)).thenReturn(true);
        when(testResultRepository.findTopByTestConfigIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> queryService.getLatestResult(1L));
    }

    @Test
    @DisplayName("toSummaryPage enriches with latest result")
    void toSummaryPage_enrichesWithLatestResult() {
        TestConfig config = buildConfig(1L);
        TestResult result = buildResult(10L);
        Page<TestConfig> page = new PageImpl<>(List.of(config));
        when(testResultRepository.findTopByTestConfigIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(result));

        Page<TestSummaryResponse> summaries = queryService.toSummaryPage(page);
        assertEquals(1, summaries.getTotalElements());
        TestSummaryResponse summary = summaries.getContent().get(0);
        assertEquals(result.getAvgResponseTimeMs(), summary.lastAvgResponseTimeMs());
    }

    private TestConfig buildConfig(Long id) {
        TestConfig config = TestConfig.builder()
                .name("Test").targetUrl("https://api.example.com")
                .httpMethod(HttpMethod.GET).testType(TestType.LOAD)
                .virtualUsers(50).durationSeconds(60).rampUpSeconds(10)
                .status(TestStatus.COMPLETED).results(new ArrayList<>()).build();
        config.setId(id);
        config.setCreatedAt(Instant.now());
        return config;
    }

    private TestResult buildResult(Long id) {
        TestResult result = TestResult.builder()
                .testConfig(buildConfig(1L)).status(TestStatus.COMPLETED)
                .totalRequests(1000).successCount(950).failureCount(50)
                .avgResponseTimeMs(45.5).minResponseTimeMs(5).maxResponseTimeMs(500)
                .p50Ms(30).p75Ms(50).p90Ms(80).p95Ms(120).p99Ms(250)
                .throughputRps(16.7).errorRate(5).totalDataBytes(500000)
                .startedAt(Instant.now()).durationMs(60000).build();
        result.setId(id);
        result.setCreatedAt(Instant.now());
        return result;
    }

}
