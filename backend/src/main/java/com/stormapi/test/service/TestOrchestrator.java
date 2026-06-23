package com.stormapi.test.service;

import com.stormapi.common.exception.InvalidTestConfigException;
import com.stormapi.common.exception.InvalidStateTransitionException;
import com.stormapi.common.exception.ResourceNotFoundException;
import com.stormapi.common.exception.TestAlreadyRunningException;
import com.stormapi.engine.analysis.EngineAnalysisResult;
import com.stormapi.engine.TestEngine;
import com.stormapi.engine.TestEngineFactory;
import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.RequestResult;
import com.stormapi.engine.http.RequestSpec;
import com.stormapi.engine.metrics.LiveMetricsSnapshot;
import com.stormapi.engine.metrics.MetricsCollector;
import com.stormapi.engine.user.ThinkTimeStrategy;
import com.stormapi.metrics.model.MetricSnapshot;
import com.stormapi.metrics.repository.MetricSnapshotRepository;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestResult;
import com.stormapi.test.model.TestStatus;
import com.stormapi.test.repository.TestConfigRepository;
import com.stormapi.test.repository.TestResultRepository;
import com.stormapi.websocket.broadcast.LiveMetricsBroadcaster;
import com.stormapi.websocket.broadcast.RequestLogBroadcaster;
import com.stormapi.websocket.broadcast.TestEventPublisher;
import com.stormapi.websocket.dto.TestEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The lifecycle coordinator for all test executions.
 *
 * Manages the full lifecycle:
 * validation → setup → execution → snapshot collection → result aggregation → persistence → cleanup
 *
 * This is the most important class in the application. It connects:
 * - Domain model (TestConfig, TestResult, MetricSnapshot)
 * - Engine layer (TestEngine, ExecutionContext, MetricsCollector)
 * - Persistence layer (Spring Data repositories)
 *
 * Thread model:
 * - {@link #startTest} runs on the Spring MVC request thread (returns immediately)
 * - The actual test runs on a dedicated virtual thread
 * - Each running test has its own snapshot timer thread
 * - Virtual user threads are managed by the engine
 *
 * Concurrency:
 * - {@code runningTests} is a ConcurrentHashMap — thread-safe for concurrent start/stop
 * - Each test run is isolated — no shared mutable state between tests
 */
@Service
public class TestOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TestOrchestrator.class);
    private static final int SNAPSHOT_FLUSH_INTERVAL = 60; // flush every 60 snapshots (~1 minute)

    private final TestConfigRepository testConfigRepository;
    private final TestResultRepository testResultRepository;
    private final MetricSnapshotRepository metricSnapshotRepository;
    private final LiveMetricsBroadcaster liveMetricsBroadcaster;
    private final RequestLogBroadcaster requestLogBroadcaster;
    private final TestEventPublisher testEventPublisher;

    private final ConcurrentHashMap<Long, RunningTestHandle> runningTests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    public TestOrchestrator(TestConfigRepository testConfigRepository,
                            TestResultRepository testResultRepository,
                            MetricSnapshotRepository metricSnapshotRepository,
                            LiveMetricsBroadcaster liveMetricsBroadcaster,
                            RequestLogBroadcaster requestLogBroadcaster,
                            TestEventPublisher testEventPublisher) {
        this.testConfigRepository = testConfigRepository;
        this.testResultRepository = testResultRepository;
        this.metricSnapshotRepository = metricSnapshotRepository;
        this.liveMetricsBroadcaster = liveMetricsBroadcaster;
        this.requestLogBroadcaster = requestLogBroadcaster;
        this.testEventPublisher = testEventPublisher;
    }

    /**
     * Starts a test execution asynchronously.
     *
     * Validates the config, creates a TestResult, and launches the engine
     * on a virtual thread. Returns the TestResult ID immediately.
     *
     * @param configId the test configuration ID
     * @return the created TestResult ID
     * @throws IllegalArgumentException if config not found or invalid
     * @throws IllegalStateException if test is already running
     */
    public Long startTest(Long configId) {
        // 1. Load and validate
        TestConfig config = testConfigRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("TestConfig", configId));

        validateConfig(config);

        if (runningTests.containsKey(configId)) {
            throw new TestAlreadyRunningException(configId);
        }

        // 2. Create TestResult with RUNNING status
        TestResult result = TestResult.builder()
                .testConfig(config)
                .status(TestStatus.RUNNING)
                .startedAt(Instant.now())
                .build();
        result = testResultRepository.save(result);

        // 3. Update config status
        config.setStatus(TestStatus.RUNNING);
        testConfigRepository.save(config);

        // 4. Register before launching to avoid race condition with isRunning()
        final Long resultId = result.getId();
        cancelFlags.put(configId, new java.util.concurrent.atomic.AtomicBoolean(false));
        runningTests.put(configId, new RunningTestHandle(null, null, resultId));

        // 5. Publish TEST_STARTED event via WebSocket
        testEventPublisher.publishEvent(configId, TestEventType.TEST_STARTED,
                "Test started",
                Map.of("resultId", resultId,
                        "testType", config.getTestType().name(),
                        "virtualUsers", config.getVirtualUsers()));

        Thread.ofVirtual()
                .name("storm-test-" + configId)
                .start(() -> runTest(configId, resultId));

        log.info("Started test for config {} → result {}", configId, resultId);
        return resultId;
    }

    /**
     * Stops a running test gracefully.
     * The engine and context are signaled to stop. The execution thread's
     * finally block handles persistence and cleanup.
     *
     * @param configId the test configuration ID
     * @throws IllegalStateException if no test is running for this config
     */
    public void stopTest(Long configId) {
        RunningTestHandle handle = runningTests.get(configId);
        if (handle == null) {
            throw new InvalidStateTransitionException("No running test for config: " + configId);
        }

        log.info("Stopping test for config {}", configId);
        cancelFlags.computeIfAbsent(configId, k -> new java.util.concurrent.atomic.AtomicBoolean()).set(true);
        if (handle.engine() != null) handle.engine().stop();
        if (handle.context() != null) handle.context().stop();

        // Publish TEST_STOPPED event via WebSocket
        testEventPublisher.publishEvent(configId, TestEventType.TEST_STOPPED,
                "Test stopped by user", Map.of());
    }

    /**
     * Returns true if a test is currently running for the given config.
     */
    public boolean isRunning(Long configId) {
        return runningTests.containsKey(configId);
    }

    // ── Private Lifecycle Methods ──────────────────────────────────

    /**
     * The main test execution flow. Runs on a virtual thread.
     * Handles the full lifecycle with guaranteed cleanup via try/finally.
     */
    private void runTest(Long configId, Long resultId) {
        ScheduledExecutorService snapshotTimer = null;
        TestConfig config = null;
        MetricsCollector metricsCollector = null;
        ExecutionContext context = null;
        TestEngine engine = null;
        List<MetricSnapshot> snapshotBuffer = new ArrayList<>();
        TestStatus finalStatus = TestStatus.COMPLETED;
        EngineAnalysisResult analysisResult = null;

        try {
            // 1. Load config (fresh read — avoid stale state)
            config = testConfigRepository.findById(configId)
                    .orElseThrow(() -> new IllegalStateException("Config disappeared: " + configId));

            // 2. Build engine components
            RequestSpec spec = RequestSpec.fromTestConfig(config);
            ThinkTimeStrategy thinkTime = ThinkTimeStrategy.fromConfig(config);

            // Use a holder so MetricsCollector can reference context.getActiveUsers()
            // before the context variable is assigned
            final java.util.concurrent.atomic.AtomicReference<ExecutionContext> contextRef =
                    new java.util.concurrent.atomic.AtomicReference<>();
            metricsCollector = new MetricsCollector(() -> {
                ExecutionContext ctx = contextRef.get();
                return ctx != null ? ctx.getActiveUsers() : 0;
            });

            // Composite consumer: feeds both MetricsCollector and RequestLogBroadcaster
            final MetricsCollector mc = metricsCollector;
            requestLogBroadcaster.startCapturing(configId);
            Consumer<RequestResult> logConsumer = requestLogBroadcaster.createConsumer(
                    configId, spec.url(), spec.method());
            Consumer<RequestResult> compositeConsumer = result -> {
                mc.recordResult(result);
                try {
                    logConsumer.accept(result);
                } catch (Exception ex) {
                    // Never let log capture failures affect metrics
                }
            };

            context = new ExecutionContext(spec, thinkTime, compositeConsumer);
            context.setMaxRetries(config.getMaxRetries());
            context.setSnapshotSupplier(metricsCollector::snapshot);
            contextRef.set(context);

            engine = TestEngineFactory.create(config.getTestType());

            // 3. Update handle with real references (placeholder was set in startTest)
            runningTests.put(configId, new RunningTestHandle(context, engine, resultId));

            // 4. Start WebSocket metrics broadcasting
            liveMetricsBroadcaster.startBroadcasting(configId);

            // 4. Load the TestResult for snapshot FK
            final TestResult testResult = testResultRepository.findById(resultId)
                    .orElseThrow(() -> new IllegalStateException("TestResult disappeared: " + resultId));

            // 5. Start snapshot timer (1 snapshot per second)
            snapshotTimer = Executors.newSingleThreadScheduledExecutor(
                    Thread.ofVirtual().name("storm-snapshot-" + configId).factory());

            final Long wsConfigId = configId;
            snapshotTimer.scheduleAtFixedRate(() -> {
                try {
                    LiveMetricsSnapshot snap = mc.snapshot();
                    MetricSnapshot entity = mapToSnapshotEntity(snap, testResult);
                    snapshotBuffer.add(entity);

                    // Broadcast metrics and logs via WebSocket
                    liveMetricsBroadcaster.broadcast(wsConfigId, snap);
                    requestLogBroadcaster.flush(wsConfigId);

                    // Emit TEST_PROGRESS every 10 seconds
                    if (snapshotBuffer.size() % 10 == 0) {
                        testEventPublisher.publishEvent(wsConfigId, TestEventType.TEST_PROGRESS,
                                "Test in progress",
                                Map.of("elapsedSeconds", snapshotBuffer.size(),
                                        "totalRequests", snap.totalRequests(),
                                        "activeUsers", snap.activeUsers()));
                    }

                    // Periodic flush for long-running tests
                    if (snapshotBuffer.size() % SNAPSHOT_FLUSH_INTERVAL == 0) {
                        flushSnapshots(snapshotBuffer);
                    }
                } catch (Exception e) {
                    log.warn("Failed to capture metric snapshot", e);
                }
            }, 1, 1, TimeUnit.SECONDS);

            // 6. Start context and execute
            context.start();
            engine.execute(context, config);

            // 7. Read engine-specific analysis result (Phase 7 engines)
            analysisResult = context.getAnalysisResult();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finalStatus = TestStatus.CANCELLED;
            log.info("Test {} was interrupted (cancelled)", configId);
        } catch (Exception e) {
            finalStatus = TestStatus.FAILED;
            log.error("Test {} failed with error: {}", configId, e.getMessage(), e);
        } finally {
            // 7. Shutdown snapshot timer
            if (snapshotTimer != null) {
                snapshotTimer.shutdown();
                try {
                    snapshotTimer.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 8. Ensure context is stopped
            if (context != null) {
                context.stop();
            }

            // 9. Check if cancellation was requested
            java.util.concurrent.atomic.AtomicBoolean cancelFlag = cancelFlags.remove(configId);
            if (cancelFlag != null && cancelFlag.get()) {
                finalStatus = TestStatus.CANCELLED;
            }

            // 10. Stop WebSocket broadcasters
            LiveMetricsSnapshot finalSnapshot = (metricsCollector != null)
                    ? metricsCollector.snapshot() : null;
            liveMetricsBroadcaster.stopBroadcasting(configId, finalSnapshot);
            requestLogBroadcaster.stopCapturing(configId);

            // 11. Publish final lifecycle event
            TestEventType finalEventType = switch (finalStatus) {
                case COMPLETED -> TestEventType.TEST_COMPLETED;
                case FAILED -> TestEventType.TEST_FAILED;
                case CANCELLED -> TestEventType.TEST_CANCELLED;
                default -> TestEventType.TEST_COMPLETED;
            };
            Map<String, Object> finalMetadata = new java.util.HashMap<>();
            finalMetadata.put("resultId", resultId);
            if (finalSnapshot != null) {
                finalMetadata.put("totalRequests", finalSnapshot.totalRequests());
                finalMetadata.put("avgResponseTimeMs", finalSnapshot.avgResponseTimeMs());
            }
            testEventPublisher.publishEvent(configId, finalEventType,
                    "Test " + finalStatus.name().toLowerCase(), finalMetadata);

            // 12. Persist results
            try {
                persistResults(resultId, configId, metricsCollector, snapshotBuffer, finalStatus, analysisResult);
            } catch (Exception e) {
                log.error("Failed to persist results for test {}: {}", configId, e.getMessage(), e);
            }

            // 13. Cleanup
            runningTests.remove(configId);
            log.info("Test {} completed with status {}", configId, finalStatus);
        }
    }

    /**
     * Persists final metrics to TestResult and flushes any remaining snapshots.
     */
    private void persistResults(Long resultId, Long configId,
                                MetricsCollector metricsCollector,
                                List<MetricSnapshot> snapshotBuffer,
                                TestStatus finalStatus,
                                EngineAnalysisResult analysisResult) {
        // Flush remaining snapshots
        if (!snapshotBuffer.isEmpty()) {
            flushSnapshots(snapshotBuffer);
        }

        // Update TestResult with final metrics
        TestResult result = testResultRepository.findById(resultId).orElse(null);
        if (result != null && metricsCollector != null) {
            LiveMetricsSnapshot finalSnapshot = metricsCollector.snapshot();
            mapSnapshotToResult(finalSnapshot, result);
            result.setStatus(finalStatus);
            result.setCompletedAt(Instant.now());
            result.setDurationMs(Duration.between(result.getStartedAt(), result.getCompletedAt()).toMillis());

            // Compute overall throughput from total elapsed time
            double elapsedSeconds = result.getDurationMs() / 1000.0;
            result.setThroughputRps(elapsedSeconds > 0
                    ? finalSnapshot.totalRequests() / elapsedSeconds
                    : 0.0);

            // Apply engine-specific analysis results
            if (analysisResult != null) {
                result.setBreakpointUsers(analysisResult.breakpointUsers());
                result.setRecoveryTimeMs(analysisResult.recoveryTimeMs());
                result.setDegradationSlope(analysisResult.degradationSlope());
                result.setDegradationDetected(analysisResult.degradationDetected());
            }

            testResultRepository.save(result);
        }

        // Update TestConfig status
        TestConfig config = testConfigRepository.findById(configId).orElse(null);
        if (config != null) {
            config.setStatus(finalStatus == TestStatus.CANCELLED ? TestStatus.CREATED : finalStatus);
            testConfigRepository.save(config);
        }
    }

    // ── Mapping Methods ────────────────────────────────────────────

    private void mapSnapshotToResult(LiveMetricsSnapshot snapshot, TestResult result) {
        result.setTotalRequests(snapshot.totalRequests());
        result.setSuccessCount(snapshot.successCount());
        result.setFailureCount(snapshot.failureCount());
        result.setAvgResponseTimeMs(snapshot.avgResponseTimeMs());
        result.setMinResponseTimeMs(snapshot.minResponseTimeMs());
        result.setMaxResponseTimeMs(snapshot.maxResponseTimeMs());
        result.setP50Ms(snapshot.p50Ms());
        result.setP75Ms(snapshot.p75Ms());
        result.setP90Ms(snapshot.p90Ms());
        result.setP95Ms(snapshot.p95Ms());
        result.setP99Ms(snapshot.p99Ms());
        result.setErrorRate(snapshot.errorRate());
        result.setTotalDataBytes(snapshot.totalDataBytes());
    }

    private MetricSnapshot mapToSnapshotEntity(LiveMetricsSnapshot snapshot, TestResult testResult) {
        return MetricSnapshot.builder()
                .testResult(testResult)
                .timestamp(snapshot.timestamp())
                .activeUsers(snapshot.activeUsers())
                .requestsPerSecond(snapshot.throughputRps())
                .avgResponseTimeMs(snapshot.avgResponseTimeMs())
                .errorRate(snapshot.errorRate())
                .p95Ms(snapshot.p95Ms())
                .cumulativeRequests(snapshot.totalRequests())
                .cumulativeErrors(snapshot.failureCount())
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────

    private void validateConfig(TestConfig config) {
        if (config.getTargetUrl() == null || config.getTargetUrl().isBlank()) {
            throw new InvalidTestConfigException("Target URL must not be empty");
        }
        if (config.getVirtualUsers() <= 0) {
            throw new InvalidTestConfigException("Virtual users must be > 0");
        }
        if (config.getDurationSeconds() <= 0) {
            throw new InvalidTestConfigException("Duration must be > 0");
        }
        if (config.getTestType() == null) {
            throw new InvalidTestConfigException("Test type must not be null");
        }
    }

    private void flushSnapshots(List<MetricSnapshot> buffer) {
        try {
            metricSnapshotRepository.saveAll(new ArrayList<>(buffer));
            buffer.clear();
        } catch (Exception e) {
            log.warn("Failed to flush metric snapshots: {}", e.getMessage());
        }
    }

    // ── Inner Record ───────────────────────────────────────────────

    /**
     * Tracks a running test's key components for stop/status operations.
     */
    private record RunningTestHandle(
            ExecutionContext context,
            TestEngine engine,
            Long testResultId
    ) {}

}
