package com.stormapi.engine.spike;

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

@DisplayName("SpikeTestEngine Tests")
class SpikeTestEngineTest {

    private TestConfig buildConfig(int baseUsers, int spikeUsers, int duration) {
        return TestConfig.builder()
                .name("spike-test")
                .targetUrl("http://localhost:9999/api")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.SPIKE)
                .virtualUsers(baseUsers)
                .spikeUsers(spikeUsers)
                .durationSeconds(duration)
                .rampUpSeconds(0)
                .timeoutMs(5000)
                .thinkTimeMs(0)
                .build();
    }

    private LiveMetricsSnapshot fakeSnapshot(double avgMs) {
        return new LiveMetricsSnapshot(
                100, 95, 5, avgMs, 10.0, 200.0,
                40.0, 60.0, 80.0, 100.0, 150.0,
                20.0, 5.0, 10, 1000L,
                Map.of(), Instant.now()
        );
    }

    @Test
    @DisplayName("getSupportedType returns SPIKE")
    void supportsType() {
        assertEquals(TestType.SPIKE, new SpikeTestEngine().getSupportedType());
    }

    @Test
    @DisplayName("Engine completes and produces analysis result")
    void completesWithAnalysisResult() throws InterruptedException {
        SpikeTestEngine engine = new SpikeTestEngine();
        TestConfig config = buildConfig(3, 5, 4);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        // Stable response — recovery should be fast
        context.setSnapshotSupplier(() -> fakeSnapshot(50.0));
        context.start();

        Thread runner = Thread.ofVirtual().start(() -> {
            try {
                engine.execute(context, config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        runner.join(30_000);
        assertFalse(runner.isAlive(), "Engine should have completed");

        EngineAnalysisResult result = context.getAnalysisResult();
        assertNotNull(result, "Analysis result should be set");
        assertNotNull(result.recoveryTimeMs(), "Recovery time should be set");
    }

    @Test
    @DisplayName("Respects stop signal")
    void respectsStopSignal() throws InterruptedException {
        SpikeTestEngine engine = new SpikeTestEngine();
        TestConfig config = buildConfig(3, 5, 20);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> fakeSnapshot(50.0));
        context.start();

        Thread runner = Thread.ofVirtual().start(() -> {
            try {
                engine.execute(context, config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(1000);
        engine.stop();
        context.stop();

        runner.join(30_000);
        assertFalse(runner.isAlive(), "Engine should have stopped");
    }

    @Test
    @DisplayName("Short test duration still completes cleanly")
    void shortDuration_completesCleanly() throws InterruptedException {
        SpikeTestEngine engine = new SpikeTestEngine();
        TestConfig config = buildConfig(2, 3, 4);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> fakeSnapshot(50.0));
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
    }

}
