package com.stormapi.dashboard.service;

import com.stormapi.dashboard.dto.DashboardStatsResponse;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock
    private TestConfigRepository testConfigRepository;

    @Mock
    private TestResultRepository testResultRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getStats returns aggregated statistics")
    void getStats_returnsAggregatedData() {
        when(testConfigRepository.count()).thenReturn(10L);
        when(testResultRepository.count()).thenReturn(25L);
        when(testConfigRepository.countByStatus(TestStatus.RUNNING)).thenReturn(2L);
        when(testConfigRepository.countByStatus(TestStatus.COMPLETED)).thenReturn(7L);
        when(testConfigRepository.countByStatus(TestStatus.FAILED)).thenReturn(1L);
        when(testResultRepository.sumTotalRequests()).thenReturn(50000L);
        when(testResultRepository.avgResponseTimeMs()).thenReturn(45.5);
        when(testResultRepository.avgThroughputRps()).thenReturn(100.0);
        when(testResultRepository.avgErrorRate()).thenReturn(2.5);
        when(testConfigRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        for (TestType type : TestType.values()) {
            when(testConfigRepository.countByTestType(type)).thenReturn(1L);
        }

        DashboardStatsResponse stats = dashboardService.getStats();

        assertEquals(10, stats.totalTests());
        assertEquals(25, stats.totalRuns());
        assertEquals(2, stats.runningTests());
        assertEquals(7, stats.completedTests());
        assertEquals(1, stats.failedTests());
        assertEquals(50000, stats.totalRequestsSent());
        assertEquals(45.5, stats.avgResponseTimeMs());
        assertEquals(100.0, stats.avgThroughputRps());
        assertEquals(2.5, stats.avgErrorRate());
        assertNotNull(stats.testTypeDistribution());
    }

    @Test
    @DisplayName("getStats with recent tests enriches with latest result")
    void getStats_enrichesRecentTests() {
        when(testConfigRepository.count()).thenReturn(1L);
        when(testResultRepository.count()).thenReturn(1L);
        when(testConfigRepository.countByStatus(any())).thenReturn(0L);
        when(testResultRepository.sumTotalRequests()).thenReturn(0L);
        when(testResultRepository.avgResponseTimeMs()).thenReturn(0.0);
        when(testResultRepository.avgThroughputRps()).thenReturn(0.0);
        when(testResultRepository.avgErrorRate()).thenReturn(0.0);
        for (TestType type : TestType.values()) {
            when(testConfigRepository.countByTestType(type)).thenReturn(0L);
        }

        TestConfig config = TestConfig.builder()
                .name("Test").targetUrl("https://api.example.com")
                .httpMethod(HttpMethod.GET).testType(TestType.LOAD)
                .virtualUsers(50).durationSeconds(60)
                .status(TestStatus.COMPLETED).results(new ArrayList<>()).build();
        config.setId(1L);
        config.setCreatedAt(Instant.now());
        when(testConfigRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(config));
        when(testResultRepository.findTopByTestConfigIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());

        DashboardStatsResponse stats = dashboardService.getStats();

        assertEquals(1, stats.recentTests().size());
        assertNull(stats.recentTests().get(0).lastAvgResponseTimeMs());
    }

}
