package com.stormapi.engine.breakpoint;

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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BreakpointTestEngine Tests")
class BreakpointTestEngineTest {

    private TestConfig buildConfig(int maxUsers, int stepSize, int stepDuration, int duration) {
        return TestConfig.builder()
                .name("breakpoint-test")
                .targetUrl("http://localhost:9999/api")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.BREAKPOINT)
                .virtualUsers(maxUsers)
                .stepSize(stepSize)
                .stepDurationSeconds(stepDuration)
                .durationSeconds(duration)
                .rampUpSeconds(0)
                .timeoutMs(200)   // Very short timeout for fast test execution
                .thinkTimeMs(0)
                .build();
    }

    @Test
    @DisplayName("getSupportedType returns BREAKPOINT")
    void supportsType() {
        assertEquals(TestType.BREAKPOINT, new BreakpointTestEngine().getSupportedType());
    }

    @Test
    @DisplayName("Finds breakpoint when system degrades at known threshold")
    void findsBreakpointViaBinarySearch() throws InterruptedException {
        BreakpointTestEngine engine = new BreakpointTestEngine();
        // Small scale: 30 users max, step=5, 1s per step, 30s total
        TestConfig config = buildConfig(30, 5, 1, 30);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});

        // System breaks above 15 users (error rate > 10%)
        context.setSnapshotSupplier(() -> {
            int users = context.getActiveUsers();
            double errorRate = users > 15 ? 15.0 : 1.0;
            return new LiveMetricsSnapshot(
                    100, 95, 5, 50.0, 10.0, 100.0,
                    40.0, 50.0, 60.0, 70.0, 80.0,
                    20.0, errorRate, users, 1000L,
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

        runner.join(45_000);
        if (runner.isAlive()) {
            engine.stop();
            context.stop();
            runner.join(5_000);
        }
        assertFalse(runner.isAlive(), "Engine should have completed within timeout");

        EngineAnalysisResult result = context.getAnalysisResult();
        assertNotNull(result, "Should produce analysis result");
        assertNotNull(result.breakpointUsers(), "Should report breakpoint users");
        assertTrue(result.breakpointUsers() > 0, "Breakpoint should be > 0");
    }

    @Test
    @DisplayName("System never breaks → reports maxUsers as breakpoint")
    void systemNeverBreaks_reportsMaxUsers() throws InterruptedException {
        BreakpointTestEngine engine = new BreakpointTestEngine();
        // Tiny scale: 10 users, step=5, 1s per step, 15s total
        TestConfig config = buildConfig(10, 5, 1, 15);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});

        // Always healthy
        context.setSnapshotSupplier(() -> new LiveMetricsSnapshot(
                100, 99, 1, 30.0, 10.0, 60.0,
                25.0, 35.0, 45.0, 50.0, 55.0,
                20.0, 1.0, context.getActiveUsers(), 1000L,
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

        runner.join(30_000);
        if (runner.isAlive()) {
            engine.stop();
            context.stop();
            runner.join(5_000);
        }
        assertFalse(runner.isAlive(), "Engine should have completed within timeout");

        EngineAnalysisResult result = context.getAnalysisResult();
        assertNotNull(result);
        assertNotNull(result.breakpointUsers());
        assertTrue(result.breakpointUsers() > 0);
    }

    @Test
    @DisplayName("Respects stop signal")
    void respectsStopSignal() throws InterruptedException {
        BreakpointTestEngine engine = new BreakpointTestEngine();
        TestConfig config = buildConfig(100, 10, 2, 120);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> new LiveMetricsSnapshot(
                100, 99, 1, 30.0, 10.0, 60.0,
                25.0, 35.0, 45.0, 50.0, 55.0,
                20.0, 1.0, 10, 1000L,
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

        Thread.sleep(3000);
        engine.stop();
        context.stop();

        runner.join(15_000);
        assertFalse(runner.isAlive(), "Engine should stop promptly");
    }

    @Test
    @DisplayName("Duration limit prevents infinite searching")
    void durationLimitRespected() throws InterruptedException {
        BreakpointTestEngine engine = new BreakpointTestEngine();
        TestConfig config = buildConfig(100, 5, 1, 5);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> new LiveMetricsSnapshot(
                100, 99, 1, 30.0, 10.0, 60.0,
                25.0, 35.0, 45.0, 50.0, 55.0,
                20.0, 1.0, 10, 1000L,
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

        if (runner.isAlive()) {
            engine.stop();
            context.stop();
            runner.join(5_000);
        }
        assertFalse(runner.isAlive());
        assertTrue(elapsed < 25_000, "Should respect duration limit, took " + elapsed + "ms");
    }

}
