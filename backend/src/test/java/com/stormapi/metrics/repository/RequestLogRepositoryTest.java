package com.stormapi.metrics.repository;

import com.stormapi.metrics.model.RequestLog;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RequestLogRepository.
 * Verifies paginated queries, success/failure counting, and batch saves.
 */
@DataJpaTest
@ActiveProfiles("test")
class RequestLogRepositoryTest {

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private TestConfigRepository testConfigRepository;

    private TestResult savedResult;

    @BeforeEach
    void setUp() {
        testConfigRepository.deleteAll();

        TestConfig config = testConfigRepository.save(TestConfig.builder()
                .name("Request Log Test")
                .targetUrl("https://api.example.com")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.LOAD)
                .virtualUsers(10)
                .durationSeconds(10)
                .build());

        savedResult = testResultRepository.save(TestResult.builder()
                .testConfig(config)
                .status(TestStatus.COMPLETED)
                .startedAt(Instant.now().minusSeconds(10))
                .completedAt(Instant.now())
                .durationMs(10000)
                .build());

        // Insert 25 logs: 20 success, 5 failure
        Instant base = Instant.now().minusSeconds(10);
        List<RequestLog> logs = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            logs.add(RequestLog.builder()
                    .testResult(savedResult)
                    .timestamp(base.plusMillis(i * 400L))
                    .url("https://api.example.com/users")
                    .method("GET")
                    .statusCode(i < 20 ? 200 : 500)
                    .responseTimeMs(50 + i * 10L)
                    .responseSize(1024)
                    .success(i < 20)
                    .errorMessage(i >= 20 ? "Internal Server Error" : null)
                    .build());
        }
        requestLogRepository.saveAll(logs);
    }

    @Test
    void findByTestResultId_withPagination_returnsPage() {
        Page<RequestLog> page = requestLogRepository
                .findByTestResultIdOrderByTimestampAsc(savedResult.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findByTestResultId_secondPage_returnsCorrectContent() {
        Page<RequestLog> page = requestLogRepository
                .findByTestResultIdOrderByTimestampAsc(savedResult.getId(), PageRequest.of(1, 10));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getNumber()).isEqualTo(1);
    }

    @Test
    void countByTestResultIdAndSuccess_correctCounts() {
        long successCount = requestLogRepository
                .countByTestResultIdAndSuccess(savedResult.getId(), true);
        long failureCount = requestLogRepository
                .countByTestResultIdAndSuccess(savedResult.getId(), false);

        assertThat(successCount).isEqualTo(20);
        assertThat(failureCount).isEqualTo(5);
    }

    @Test
    void batchSaveAll_persists100Logs() {
        requestLogRepository.deleteAll();

        List<RequestLog> batch = new ArrayList<>();
        Instant base = Instant.now();
        for (int i = 0; i < 100; i++) {
            batch.add(RequestLog.builder()
                    .testResult(savedResult)
                    .timestamp(base.plusMillis(i))
                    .url("https://api.example.com/batch")
                    .method("POST")
                    .statusCode(201)
                    .responseTimeMs(30)
                    .responseSize(512)
                    .success(true)
                    .build());
        }

        requestLogRepository.saveAll(batch);

        assertThat(requestLogRepository.count()).isEqualTo(100);
    }

    @Test
    void deleteByTestResultId_removesAllLogs() {
        assertThat(requestLogRepository.count()).isEqualTo(25);

        requestLogRepository.deleteByTestResultId(savedResult.getId());

        assertThat(requestLogRepository.count()).isEqualTo(0);
    }

}
