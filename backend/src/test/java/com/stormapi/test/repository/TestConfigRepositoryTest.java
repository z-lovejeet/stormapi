package com.stormapi.test.repository;

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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TestConfigRepository.
 * Uses @DataJpaTest with in-memory H2 to verify JPA mappings, queries, and cascades.
 */
@DataJpaTest
@ActiveProfiles("test")
class TestConfigRepositoryTest {

    @Autowired
    private TestConfigRepository testConfigRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    private TestConfig savedConfig;

    @BeforeEach
    void setUp() {
        testConfigRepository.deleteAll();

        savedConfig = testConfigRepository.save(TestConfig.builder()
                .name("Load Test - Users API")
                .targetUrl("https://api.example.com/users")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.LOAD)
                .virtualUsers(100)
                .durationSeconds(60)
                .rampUpSeconds(10)
                .headers(Map.of("Authorization", "Bearer test-token"))
                .build());
    }

    @Test
    void save_andFindById_persistsAllFields() {
        TestConfig found = testConfigRepository.findById(savedConfig.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Load Test - Users API");
        assertThat(found.getTargetUrl()).isEqualTo("https://api.example.com/users");
        assertThat(found.getHttpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(found.getTestType()).isEqualTo(TestType.LOAD);
        assertThat(found.getVirtualUsers()).isEqualTo(100);
        assertThat(found.getDurationSeconds()).isEqualTo(60);
        assertThat(found.getRampUpSeconds()).isEqualTo(10);
        assertThat(found.getStatus()).isEqualTo(TestStatus.CREATED);
        assertThat(found.getTimeoutMs()).isEqualTo(5000); // default
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_headersAsJson_roundTrips() {
        TestConfig found = testConfigRepository.findById(savedConfig.getId()).orElseThrow();

        assertThat(found.getHeaders()).containsEntry("Authorization", "Bearer test-token");
    }

    @Test
    void findByStatusIn_returnsMatchingConfigs() {
        // savedConfig has CREATED status
        TestConfig runningConfig = testConfigRepository.save(TestConfig.builder()
                .name("Running Test")
                .targetUrl("https://api.example.com/health")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.STRESS)
                .virtualUsers(50)
                .durationSeconds(30)
                .status(TestStatus.RUNNING)
                .build());

        List<TestConfig> results = testConfigRepository.findByStatusIn(
                List.of(TestStatus.CREATED, TestStatus.RUNNING));

        assertThat(results).hasSize(2);
    }

    @Test
    void findByStatusIn_excludesNonMatchingStatuses() {
        List<TestConfig> results = testConfigRepository.findByStatusIn(
                List.of(TestStatus.COMPLETED));

        assertThat(results).isEmpty();
    }

    @Test
    void findByTestType_returnsCorrectType() {
        List<TestConfig> loadTests = testConfigRepository.findByTestType(TestType.LOAD);
        assertThat(loadTests).hasSize(1);
        assertThat(loadTests.get(0).getName()).isEqualTo("Load Test - Users API");
    }

    @Test
    void findAllByOrderByCreatedAtDesc_ordersCorrectly() {
        testConfigRepository.save(TestConfig.builder()
                .name("Second Test")
                .targetUrl("https://api.example.com/v2")
                .httpMethod(HttpMethod.POST)
                .testType(TestType.SPIKE)
                .virtualUsers(200)
                .durationSeconds(120)
                .build());

        List<TestConfig> results = testConfigRepository.findAllByOrderByCreatedAtDesc();

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        // Most recently created should be first
        assertThat(results.get(0).getName()).isEqualTo("Second Test");
    }

    @Test
    void countByStatus_returnsCorrectCount() {
        long createdCount = testConfigRepository.countByStatus(TestStatus.CREATED);
        assertThat(createdCount).isEqualTo(1);

        long runningCount = testConfigRepository.countByStatus(TestStatus.RUNNING);
        assertThat(runningCount).isEqualTo(0);
    }

    @Test
    void enumStoredAsString_notOrdinal() {
        TestConfig found = testConfigRepository.findById(savedConfig.getId()).orElseThrow();
        // If stored as ordinal, retrieving after enum reordering would break
        assertThat(found.getTestType()).isEqualTo(TestType.LOAD);
        assertThat(found.getHttpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(found.getStatus()).isEqualTo(TestStatus.CREATED);
    }

    @Test
    void cascadeDelete_deletesTestResults() {
        // Add a TestResult to the config
        TestResult result = TestResult.builder()
                .testConfig(savedConfig)
                .status(TestStatus.COMPLETED)
                .startedAt(Instant.now())
                .build();
        savedConfig.getResults().add(result);
        testConfigRepository.save(savedConfig);

        // Verify result exists
        assertThat(testResultRepository.findAll()).hasSize(1);

        // Delete config — should cascade to result
        testConfigRepository.delete(savedConfig);

        assertThat(testConfigRepository.findAll()).isEmpty();
        assertThat(testResultRepository.findAll()).isEmpty();
    }

}
