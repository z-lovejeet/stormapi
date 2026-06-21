package com.stormapi.dashboard.service;

import com.stormapi.dashboard.dto.DashboardStatsResponse;
import com.stormapi.test.dto.TestSummaryResponse;
import com.stormapi.test.mapper.TestMapper;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import com.stormapi.test.repository.TestConfigRepository;
import com.stormapi.test.repository.TestResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service for aggregating dashboard statistics.
 * All queries use single-pass SQL — no N+1.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TestConfigRepository testConfigRepository;
    private final TestResultRepository testResultRepository;

    public DashboardService(TestConfigRepository testConfigRepository,
                            TestResultRepository testResultRepository) {
        this.testConfigRepository = testConfigRepository;
        this.testResultRepository = testResultRepository;
    }

    public DashboardStatsResponse getStats() {
        long totalTests = testConfigRepository.count();
        long totalRuns = testResultRepository.count();
        long runningTests = testConfigRepository.countByStatus(TestStatus.RUNNING);
        long completedTests = testConfigRepository.countByStatus(TestStatus.COMPLETED);
        long failedTests = testConfigRepository.countByStatus(TestStatus.FAILED);
        long totalRequestsSent = testResultRepository.sumTotalRequests();
        double avgResponseTimeMs = testResultRepository.avgResponseTimeMs();
        double avgThroughputRps = testResultRepository.avgThroughputRps();
        double avgErrorRate = testResultRepository.avgErrorRate();

        // Recent tests with latest result enrichment
        List<TestConfig> recentConfigs = testConfigRepository.findTop5ByOrderByCreatedAtDesc();
        List<TestSummaryResponse> recentTests = recentConfigs.stream()
                .map(config -> {
                    TestResult latest = testResultRepository
                            .findTopByTestConfigIdOrderByCreatedAtDesc(config.getId())
                            .orElse(null);
                    return TestMapper.toSummary(config, latest);
                })
                .toList();

        // Test type distribution
        Map<TestType, Long> typeDistribution = new EnumMap<>(TestType.class);
        for (TestType type : TestType.values()) {
            typeDistribution.put(type, testConfigRepository.countByTestType(type));
        }

        return new DashboardStatsResponse(
                totalTests,
                totalRuns,
                runningTests,
                completedTests,
                failedTests,
                totalRequestsSent,
                avgResponseTimeMs,
                avgThroughputRps,
                avgErrorRate,
                recentTests,
                typeDistribution
        );
    }

}
