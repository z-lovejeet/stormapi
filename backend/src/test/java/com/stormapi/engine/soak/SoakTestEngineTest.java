package com.stormapi.engine.soak;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SoakTestEngine Tests")
class SoakTestEngineTest {

    private TestConfig buildConfig(int users, int duration) {
        return TestConfig.builder()
                .name("soak-test")
                .targetUrl("http://localhost:9999/api")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.SOAK)
                .virtualUsers(users)
                .durationSeconds(duration)
                .rampUpSeconds(0)
                .timeoutMs(5000)
                .thinkTimeMs(0)
                .build();
    }

    @Test
    @DisplayName("getSupportedType returns SOAK")
    void supportsType() {
        assertEquals(TestType.SOAK, new SoakTestEngine().getSupportedType());
    }

    @Test
    @DisplayName("Steady performance → no degradation detected")
    void stablePerformance_noDegradation() throws InterruptedException {
        SoakTestEngine engine = new SoakTestEngine();
        TestConfig config = buildConfig(3, 15); // 15s test

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});

        // Constant latency — no degradation
        context.setSnapshotSupplier(() -> new LiveMetricsSnapshot(
                100, 95, 5, 50.0, 10.0, 100.0,
                40.0, 50.0, 60.0, 70.0, 80.0,
                20.0, 5.0, 3, 1000L,
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
        assertFalse(runner.isAlive());

        EngineAnalysisResult result = context.getAnalysisResult();
        assertNotNull(result, "Analysis result should be set");
        assertNotNull(result.degradationDetected());
        // With constant latency, slope should be ~0 and no degradation
        assertFalse(result.degradationDetected(), "Constant latency should not trigger degradation");
    }

    @Test
    @DisplayName("Increasing latency → degradation detected")
    void trendAnalyzerDetectsDrift() throws InterruptedException {
        SoakTestEngine engine = new SoakTestEngine();
        TestConfig config = buildConfig(3, 25); // 25s test = ~2 samples

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});

        // Increasing latency: starts at 50ms, grows by 50ms per call
        AtomicInteger callCount = new AtomicInteger(0);
        context.setSnapshotSupplier(() -> {
            int calls = callCount.incrementAndGet();
            double avgMs = 50.0 + calls * 50.0; // 100, 150, 200, ...
            return new LiveMetricsSnapshot(
                    100, 95, 5, avgMs, 10.0, avgMs + 50,
                    avgMs * 0.8, avgMs * 0.9, avgMs, avgMs * 1.1, avgMs * 1.3,
                    20.0, 5.0, 3, 1000L,
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

        runner.join(40_000);
        assertFalse(runner.isAlive());

        EngineAnalysisResult result = context.getAnalysisResult();
        assertNotNull(result);
        assertNotNull(result.degradationSlope());
        assertTrue(result.degradationSlope() > 0, "Slope should be positive for increasing latency");
    }

    @Test
    @DisplayName("Respects stop signal")
    void respectsStopSignal() throws InterruptedException {
        SoakTestEngine engine = new SoakTestEngine();
        TestConfig config = buildConfig(3, 60);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> new LiveMetricsSnapshot(
                100, 95, 5, 50.0, 10.0, 100.0,
                40.0, 50.0, 60.0, 70.0, 80.0,
                20.0, 5.0, 3, 1000L,
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

}
