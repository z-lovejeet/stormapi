package com.stormapi.test.repository;

import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.model.RequestLog;
import com.stormapi.metrics.repository.MetricSnapshotRepository;
import com.stormapi.metrics.repository.RequestLogRepository;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TestResultRepository.
 * Verifies relationship mapping, query correctness, and cascade behavior.
 */
@DataJpaTest
@ActiveProfiles("test")
class TestResultRepositoryTest {

    @Autowired
    private TestConfigRepository testConfigRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private MetricSnapshotRepository metricSnapshotRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    private TestConfig savedConfig;
    private TestResult savedResult;

    @BeforeEach
    void setUp() {
        testConfigRepository.deleteAll();

        savedConfig = testConfigRepository.save(TestConfig.builder()
                .name("Test Config")
                .targetUrl("https://api.example.com")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.LOAD)
                .virtualUsers(50)
                .durationSeconds(30)
                .build());

        savedResult = testResultRepository.save(TestResult.builder()
                .testConfig(savedConfig)
                .status(TestStatus.COMPLETED)
                .totalRequests(1000)
                .successCount(950)
                .failureCount(50)
                .avgResponseTimeMs(120.5)
                .minResponseTimeMs(10.0)
                .maxResponseTimeMs(5000.0)
                .p50Ms(85.0)
                .p75Ms(150.0)
                .p90Ms(250.0)
                .p95Ms(400.0)
                .p99Ms(1200.0)
                .throughputRps(33.3)
                .errorRate(5.0)
                .totalDataBytes(2048000)
                .startedAt(Instant.now().minusSeconds(30))
                .completedAt(Instant.now())
                .durationMs(30000)
                .build());
    }

    @Test
    void save_persistsAllFields() {
        TestResult found = testResultRepository.findById(savedResult.getId()).orElseThrow();

        assertThat(found.getTotalRequests()).isEqualTo(1000);
        assertThat(found.getSuccessCount()).isEqualTo(950);
        assertThat(found.getP95Ms()).isEqualTo(400.0);
        assertThat(found.getThroughputRps()).isEqualTo(33.3);
        assertThat(found.getErrorRate()).isEqualTo(5.0);
        assertThat(found.getStartedAt()).isNotNull();
        assertThat(found.getCompletedAt()).isNotNull();
    }

    @Test
    void findByTestConfigIdOrderByCreatedAtDesc_returnsOrdered() {
        // Add a second result
        testResultRepository.save(TestResult.builder()
                .testConfig(savedConfig)
                .status(TestStatus.COMPLETED)
                .startedAt(Instant.now())
                .build());

        List<TestResult> results = testResultRepository
                .findByTestConfigIdOrderByCreatedAtDesc(savedConfig.getId());

        assertThat(results).hasSize(2);
    }

    @Test
    void findTopByTestConfigIdOrderByCreatedAtDesc_returnsLatest() {
        // Add a second (newer) result
        TestResult newerResult = testResultRepository.save(TestResult.builder()
                .testConfig(savedConfig)
                .status(TestStatus.FAILED)
                .startedAt(Instant.now())
                .build());

        Optional<TestResult> latest = testResultRepository
                .findTopByTestConfigIdOrderByCreatedAtDesc(savedConfig.getId());

        assertThat(latest).isPresent();
        assertThat(latest.get().getId()).isEqualTo(newerResult.getId());
    }

    @Test
    void findByStatus_filtersCorrectly() {
        List<TestResult> completed = testResultRepository.findByStatus(TestStatus.COMPLETED);
        assertThat(completed).hasSize(1);

        List<TestResult> failed = testResultRepository.findByStatus(TestStatus.FAILED);
        assertThat(failed).isEmpty();
    }

    @Test
    void nullableFields_breakpointUsersAndCompletedAt() {
        TestResult running = testResultRepository.save(TestResult.builder()
                .testConfig(savedConfig)
                .status(TestStatus.RUNNING)
                .startedAt(Instant.now())
                .build());

        TestResult found = testResultRepository.findById(running.getId()).orElseThrow();
        assertThat(found.getCompletedAt()).isNull();
        assertThat(found.getBreakpointUsers()).isNull();
    }

    @Test
    void cascadeDelete_removesMetricSnapshotsAndRequestLogs() {
        // Add a MetricSnapshot
        MetricSnapshot snapshot = MetricSnapshot.builder()
                .testResult(savedResult)
                .timestamp(Instant.now())
                .activeUsers(50)
                .requestsPerSecond(33.3)
                .avgResponseTimeMs(120.0)
                .errorRate(5.0)
                .p95Ms(400.0)
                .cumulativeRequests(500)
                .cumulativeErrors(25)
                .build();
        savedResult.getMetricSnapshots().add(snapshot);

        // Add a RequestLog
        RequestLog log = RequestLog.builder()
                .testResult(savedResult)
                .timestamp(Instant.now())
                .url("https://api.example.com")
                .method("GET")
                .statusCode(200)
                .responseTimeMs(85)
                .responseSize(1024)
                .success(true)
                .build();
        savedResult.getRequestLogs().add(log);

        testResultRepository.save(savedResult);

        // Verify children exist
        assertThat(metricSnapshotRepository.findAll()).hasSize(1);
        assertThat(requestLogRepository.findAll()).hasSize(1);

        // Delete result — should cascade
        savedConfig.getResults().remove(savedResult);
        testConfigRepository.save(savedConfig);
        testResultRepository.delete(savedResult);

        assertThat(metricSnapshotRepository.findAll()).isEmpty();
        assertThat(requestLogRepository.findAll()).isEmpty();
    }

}
