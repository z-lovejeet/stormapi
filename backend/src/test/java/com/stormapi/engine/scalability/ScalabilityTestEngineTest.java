package com.stormapi.engine.scalability;

import com.stormapi.engine.analysis.EngineAnalysisResult;
import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.RequestSpec;
import com.stormapi.engine.metrics.LiveMetricsSnapshot;
import com.stormapi.engine.user.NoThinkTimeStrategy;
import com.stormapi.engine.user.ThinkTimeStrategy;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScalabilityTestEngine Tests")
class ScalabilityTestEngineTest {

    private TestConfig buildConfig(int maxUsers, int stepSize, int stepDuration, int duration) {
        return TestConfig.builder()
                .name("scalability-test")
                .targetUrl("http://localhost:9999/api")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.SCALABILITY)
                .virtualUsers(maxUsers)
                .stepSize(stepSize)
                .stepDurationSeconds(stepDuration)
                .durationSeconds(duration)
                .rampUpSeconds(0)
                .timeoutMs(5000)
                .thinkTimeMs(0)
                .build();
    }

    @Test
    @DisplayName("getSupportedType returns SCALABILITY")
    void supportsType() {
        assertEquals(TestType.SCALABILITY, new ScalabilityTestEngine().getSupportedType());
    }

    @Test
    @DisplayName("Collects metrics per step")
    void collectsMetricsPerStep() throws InterruptedException {
        ScalabilityTestEngine engine = new ScalabilityTestEngine();
        TestConfig config = buildConfig(9, 3, 7, 60); // 3 steps: 3, 6, 9 users

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});

        // Increasing request count to simulate throughput
        AtomicLong requests = new AtomicLong(0);
        context.setSnapshotSupplier(() -> {
            long total = requests.addAndGet(50);
            return new LiveMetricsSnapshot(
                    total, total - 5, 5, 50.0, 10.0, 100.0,
                    40.0, 50.0, 60.0, 70.0, 80.0,
                    20.0, 5.0, context.getActiveUsers(), 1000L,
                    Map.of(), Instant.now()
            );
        });
        context.start();

        Thread runner = Thread.ofVirtual().start(() -> {
            try {
                engine.execute(context, config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        runner.join(90_000);
        assertFalse(runner.isAlive());

        EngineAnalysisResult result = context.getAnalysisResult();
        assertNotNull(result, "Analysis result should be set");
        assertNotNull(result.scalabilityCurve(), "Scalability curve should be set");
        assertTrue(result.scalabilityCurve().size() >= 2,
                "Should have at least 2 scalability points, got " + result.scalabilityCurve().size());
    }

    @Test
    @DisplayName("Throughput captured per step has positive values")
    void throughputCapturedPerStep() throws InterruptedException {
        ScalabilityTestEngine engine = new ScalabilityTestEngine();
        TestConfig config = buildConfig(6, 3, 7, 30);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});

        AtomicLong requests = new AtomicLong(100);
        context.setSnapshotSupplier(() -> {
            long total = requests.addAndGet(100);
            return new LiveMetricsSnapshot(
                    total, total, 0, 30.0, 5.0, 60.0,
                    25.0, 35.0, 45.0, 50.0, 55.0,
                    50.0, 0.0, context.getActiveUsers(), 5000L,
                    Map.of(), Instant.now()
            );
        });
        context.start();

        Thread runner = Thread.ofVirtual().start(() -> {
            try {
                engine.execute(context, config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        runner.join(60_000);
        assertFalse(runner.isAlive());

        EngineAnalysisResult result = context.getAnalysisResult();
        assertNotNull(result);
        if (result.scalabilityCurve() != null) {
            for (var point : result.scalabilityCurve()) {
                assertTrue(point.users() > 0, "Users should be > 0");
                assertTrue(point.throughputRps() >= 0, "Throughput should be >= 0");
            }
        }
    }

    @Test
    @DisplayName("Respects stop signal")
    void respectsStopSignal() throws InterruptedException {
        ScalabilityTestEngine engine = new ScalabilityTestEngine();
        TestConfig config = buildConfig(100, 5, 10, 120);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> new LiveMetricsSnapshot(
                100, 99, 1, 30.0, 5.0, 60.0,
                25.0, 35.0, 45.0, 50.0, 55.0,
                20.0, 1.0, 5, 1000L,
                Map.of(), Instant.now()
        ));
        context.start();

        Thread runner = Thread.ofVirtual().start(() -> {
            try {
                engine.execute(context, config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(2000);
        engine.stop();
        context.stop();

        runner.join(10_000);
        assertFalse(runner.isAlive());
    }

    @Test
    @DisplayName("Duration limit respected")
    void durationLimitRespected() throws InterruptedException {
        ScalabilityTestEngine engine = new ScalabilityTestEngine();
        TestConfig config = buildConfig(100, 5, 10, 5); // Only 5s — can't complete all steps

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> new LiveMetricsSnapshot(
                100, 99, 1, 30.0, 5.0, 60.0,
                25.0, 35.0, 45.0, 50.0, 55.0,
                20.0, 1.0, 5, 1000L,
                Map.of(), Instant.now()
        ));
        context.start();

        long startTime = System.currentTimeMillis();
        Thread runner = Thread.ofVirtual().start(() -> {
            try {
                engine.execute(context, config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        runner.join(30_000);
        long elapsed = System.currentTimeMillis() - startTime;

        assertFalse(runner.isAlive());
        assertTrue(elapsed < 20_000, "Should respect duration limit, took " + elapsed + "ms");

        // Should still produce a partial curve
        EngineAnalysisResult result = context.getAnalysisResult();
        assertNotNull(result);
    }

}
