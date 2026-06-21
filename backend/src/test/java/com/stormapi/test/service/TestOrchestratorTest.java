package com.stormapi.test.service;

import com.stormapi.common.exception.InvalidStateTransitionException;
import com.stormapi.common.exception.InvalidTestConfigException;
import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.common.exception.TestAlreadyRunningException;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.repository.MetricSnapshotRepository;
import com.stormapi.test.model.*;
import com.stormapi.test.repository.TestConfigRepository;
import com.stormapi.test.repository.TestResultRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TestOrchestrator Integration Tests")
class TestOrchestratorTest {

    @Autowired
    private TestOrchestrator orchestrator;

    @Autowired
    private TestConfigRepository testConfigRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private MetricSnapshotRepository metricSnapshotRepository;

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) wireMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
        wireMock.stubFor(get(urlEqualTo("/api/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\":\"ok\"}")
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(10)));
    }

    private TestConfig createConfig(int users, int durationSec, int rampUpSec) {
        TestConfig config = TestConfig.builder()
                .name("Integration Test")
                .description("Automated test")
                .targetUrl("http://localhost:" + wireMock.port() + "/api/test")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.LOAD)
                .virtualUsers(users)
                .durationSeconds(durationSec)
                .rampUpSeconds(rampUpSec)
                .timeoutMs(2000)
                .thinkTimeMs(0)
                .build();
        return testConfigRepository.save(config);
    }

    // ── Happy Path ─────────────────────────────────────────────────

    @Test
    @Timeout(20)
    @DisplayName("startTest — creates result, executes, persists metrics")
    void startTest_completesWithMetrics() throws InterruptedException {
        TestConfig config = createConfig(5, 3, 0);
        Long resultId = orchestrator.startTest(config.getId());

        assertNotNull(resultId, "Should return a TestResult ID");

        // Wait for test to complete
        waitForCompletion(config.getId(), 15000);

        // Verify TestResult
        TestResult result = testResultRepository.findById(resultId).orElseThrow();
        assertEquals(TestStatus.COMPLETED, result.getStatus(), "Status should be COMPLETED");
        assertTrue(result.getTotalRequests() > 0, "Should have sent requests");
        assertTrue(result.getSuccessCount() > 0, "Should have successful requests");
        assertTrue(result.getAvgResponseTimeMs() > 0, "Avg response time should be > 0");
        assertTrue(result.getThroughputRps() > 0, "Throughput should be > 0");
        assertNotNull(result.getStartedAt());
        assertNotNull(result.getCompletedAt());
        assertTrue(result.getDurationMs() > 0, "Duration should be > 0");

        // Verify MetricSnapshots
        List<MetricSnapshot> snapshots = metricSnapshotRepository
                .findByTestResultIdOrderByTimestampAsc(resultId);
        assertTrue(snapshots.size() >= 1, "Should have at least 1 snapshot, got: " + snapshots.size());

        // Verify TestConfig status reset
        TestConfig updatedConfig = testConfigRepository.findById(config.getId()).orElseThrow();
        assertEquals(TestStatus.COMPLETED, updatedConfig.getStatus());
    }

    @Test
    @Timeout(15)
    @DisplayName("startTest — percentile values are populated")
    void startTest_percentilesPopulated() throws InterruptedException {
        TestConfig config = createConfig(3, 3, 0);
        Long resultId = orchestrator.startTest(config.getId());
        waitForCompletion(config.getId(), 12000);

        TestResult result = testResultRepository.findById(resultId).orElseThrow();
        assertEquals(TestStatus.COMPLETED, result.getStatus());
        assertTrue(result.getP50Ms() > 0, "P50 should be > 0");
        assertTrue(result.getP95Ms() > 0, "P95 should be > 0");
        assertTrue(result.getP99Ms() > 0, "P99 should be > 0");
    }

    // ── Cancellation ───────────────────────────────────────────────

    @Test
    @Timeout(15)
    @DisplayName("stopTest — cancels running test and persists partial results")
    void stopTest_cancelsAndPersists() throws InterruptedException {
        TestConfig config = createConfig(5, 30, 0); // 30s — long test
        Long resultId = orchestrator.startTest(config.getId());

        // Let it run for 2 seconds, then stop
        Thread.sleep(2000);
        assertTrue(orchestrator.isRunning(config.getId()), "Should be running");
        orchestrator.stopTest(config.getId());

        // Wait for shutdown
        waitForCompletion(config.getId(), 10000);

        TestResult result = testResultRepository.findById(resultId).orElseThrow();
        assertEquals(TestStatus.CANCELLED, result.getStatus(), "Status should be CANCELLED");
        assertTrue(result.getTotalRequests() > 0, "Should have partial results");
        assertNotNull(result.getCompletedAt());
    }

    // ── Validation ─────────────────────────────────────────────────

    @Test
    @DisplayName("startTest — invalid URL throws IllegalArgumentException")
    void startTest_invalidUrl_throws() {
        TestConfig config = TestConfig.builder()
                .name("Bad Test").targetUrl("").httpMethod(HttpMethod.GET)
                .testType(TestType.LOAD).virtualUsers(1).durationSeconds(1).build();
        config = testConfigRepository.save(config);

        Long configId = config.getId();
        assertThrows(InvalidTestConfigException.class, () -> orchestrator.startTest(configId));
    }

    @Test
    @DisplayName("startTest — zero users throws InvalidTestConfigException")
    void startTest_zeroUsers_throws() {
        TestConfig config = TestConfig.builder()
                .name("Bad Test").targetUrl("http://localhost:9999")
                .httpMethod(HttpMethod.GET).testType(TestType.LOAD)
                .virtualUsers(0).durationSeconds(1).build();
        config = testConfigRepository.save(config);

        Long configId = config.getId();
        assertThrows(InvalidTestConfigException.class, () -> orchestrator.startTest(configId));
    }

    @Test
    @DisplayName("startTest — nonexistent config throws ResourceNotFoundException")
    void startTest_configNotFound_throws() {
        assertThrows(ResourceNotFoundException.class, () -> orchestrator.startTest(999999L));
    }

    @Test
    @Timeout(15)
    @DisplayName("startTest — duplicate start throws TestAlreadyRunningException")
    void startTest_alreadyRunning_throws() throws InterruptedException {
        TestConfig config = createConfig(3, 10, 0);
        orchestrator.startTest(config.getId());

        // Wait briefly for the test to start
        Thread.sleep(500);

        Long configId = config.getId();
        assertThrows(TestAlreadyRunningException.class, () -> orchestrator.startTest(configId));

        // Cleanup
        orchestrator.stopTest(config.getId());
        waitForCompletion(config.getId(), 10000);
    }

    // ── Stop Edge Cases ────────────────────────────────────────────

    @Test
    @DisplayName("stopTest — no running test throws InvalidStateTransitionException")
    void stopTest_noRunningTest_throws() {
        assertThrows(InvalidStateTransitionException.class, () -> orchestrator.stopTest(99999L));
    }

    @Test
    @Timeout(15)
    @DisplayName("isRunning — returns correct state")
    void isRunning_correctState() throws InterruptedException {
        TestConfig config = createConfig(3, 10, 0);
        assertFalse(orchestrator.isRunning(config.getId()));

        orchestrator.startTest(config.getId());
        Thread.sleep(500);
        assertTrue(orchestrator.isRunning(config.getId()));

        orchestrator.stopTest(config.getId());
        waitForCompletion(config.getId(), 10000);
        assertFalse(orchestrator.isRunning(config.getId()));
    }

    // ── Helpers ────────────────────────────────────────────────────

    private void waitForCompletion(Long configId, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (orchestrator.isRunning(configId) && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        // Small grace period for persistence
        Thread.sleep(500);
    }

}
