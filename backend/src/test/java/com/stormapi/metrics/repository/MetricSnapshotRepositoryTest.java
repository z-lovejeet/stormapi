package com.stormapi.metrics.repository;

import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.model.TestType;
import com.stormapi.test.repository.TestConfigRepository;
import com.stormapi.test.repository.TestResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MetricSnapshotRepository.
 * Verifies time-ordered queries and bulk delete.
 */
@DataJpaTest
@ActiveProfiles("test")
class MetricSnapshotRepositoryTest {

    @Autowired
    private MetricSnapshotRepository metricSnapshotRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private TestConfigRepository testConfigRepository;

    private TestResult savedResult;

    @BeforeEach
    void setUp() {
        testConfigRepository.deleteAll();

        TestConfig config = testConfigRepository.save(TestConfig.builder()
                .name("Snapshot Test")
                .targetUrl("https://api.example.com")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.LOAD)
                .virtualUsers(10)
                .durationSeconds(5)
                .build());

        savedResult = testResultRepository.save(TestResult.builder()
                .testConfig(config)
                .status(TestStatus.COMPLETED)
                .startedAt(Instant.now().minusSeconds(5))
                .completedAt(Instant.now())
                .durationMs(5000)
                .build());

        // Insert snapshots out of order to verify ordering
        Instant base = Instant.now().minusSeconds(5);
        for (int i = 4; i >= 0; i--) {
            metricSnapshotRepository.save(MetricSnapshot.builder()
                    .testResult(savedResult)
                    .timestamp(base.plusSeconds(i))
                    .activeUsers(10)
                    .requestsPerSecond(20.0 + i)
                    .avgResponseTimeMs(100.0 + i * 10)
                    .errorRate(1.0)
                    .p95Ms(200.0)
                    .cumulativeRequests(i * 20L)
                    .cumulativeErrors(i)
                    .build());
        }
    }

    @Test
    void findByTestResultIdOrderByTimestampAsc_returnsOrdered() {
        List<MetricSnapshot> snapshots = metricSnapshotRepository
                .findByTestResultIdOrderByTimestampAsc(savedResult.getId());

        assertThat(snapshots).hasSize(5);
        // Verify ascending order
        for (int i = 1; i < snapshots.size(); i++) {
            assertThat(snapshots.get(i).getTimestamp())
                    .isAfterOrEqualTo(snapshots.get(i - 1).getTimestamp());
        }
    }

    @Test
    void deleteByTestResultId_removesAllSnapshots() {
        assertThat(metricSnapshotRepository.findAll()).hasSize(5);

        metricSnapshotRepository.deleteByTestResultId(savedResult.getId());

        assertThat(metricSnapshotRepository.findAll()).isEmpty();
    }

    @Test
    void save_persistsAllFields() {
        List<MetricSnapshot> snapshots = metricSnapshotRepository
                .findByTestResultIdOrderByTimestampAsc(savedResult.getId());

        MetricSnapshot first = snapshots.get(0);
        assertThat(first.getActiveUsers()).isEqualTo(10);
        assertThat(first.getRequestsPerSecond()).isGreaterThan(0);
        assertThat(first.getAvgResponseTimeMs()).isGreaterThan(0);
        assertThat(first.getTimestamp()).isNotNull();
    }

}
