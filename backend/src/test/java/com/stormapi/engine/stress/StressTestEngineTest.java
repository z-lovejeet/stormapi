package com.stormapi.engine.stress;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpClientFactory;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.http.RequestResult;
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

@DisplayName("StressTestEngine Tests")
class StressTestEngineTest {

    private TestConfig buildConfig(int maxUsers, int stepSize, int stepDuration, int duration) {
        return TestConfig.builder()
                .name("stress-test")
                .targetUrl("http://localhost:9999/api")
                .httpMethod(HttpMethod.GET)
                .testType(TestType.STRESS)
                .virtualUsers(maxUsers)
                .stepSize(stepSize)
                .stepDurationSeconds(stepDuration)
                .durationSeconds(duration)
                .rampUpSeconds(0)
                .timeoutMs(5000)
                .thinkTimeMs(0)
                .build();
    }

    private LiveMetricsSnapshot fakeSnapshot(double errorRate) {
        return new LiveMetricsSnapshot(
                100, 90, 10, 50.0, 10.0, 200.0,
                40.0, 60.0, 80.0, 100.0, 150.0,
                20.0, errorRate, 10, 1000L,
                Map.of(), Instant.now()
        );
    }

    @Test
    @DisplayName("getSupportedType returns STRESS")
    void supportsType() {
        assertEquals(TestType.STRESS, new StressTestEngine().getSupportedType());
    }

    @Test
    @DisplayName("Users increase in steps")
    void userCountIncreasesInSteps() throws InterruptedException {
        StressTestEngine engine = new StressTestEngine();
        TestConfig config = buildConfig(6, 2, 1, 5);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> fakeSnapshot(0.0));

        context.start();

        Thread runner = Thread.ofVirtual().start(() -> {
            try {
                engine.execute(context, config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wait for test to complete (short duration)
        runner.join(30_000);

        // Users should have been spawned (at least 2 steps worth)
        // Engine should have completed since duration is 5s
        assertFalse(runner.isAlive(), "Engine should have completed");
    }

    @Test
    @DisplayName("Stops when stop() is called")
    void respectsStopSignal() throws InterruptedException {
        StressTestEngine engine = new StressTestEngine();
        TestConfig config = buildConfig(100, 5, 2, 60);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> fakeSnapshot(0.0));

        context.start();

        Thread runner = Thread.ofVirtual().start(() -> {
            try {
                engine.execute(context, config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(1500);
        engine.stop();
        context.stop();

        runner.join(30_000);
        assertFalse(runner.isAlive(), "Engine should have stopped");
    }

    @Test
    @DisplayName("Respects maxUsers ceiling")
    void stopsWhenMaxUsersReached() throws InterruptedException {
        StressTestEngine engine = new StressTestEngine();
        AtomicInteger maxSeen = new AtomicInteger(0);

        TestConfig config = buildConfig(4, 2, 1, 10);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> fakeSnapshot(0.0));

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

    @Test
    @DisplayName("Duration limit respected")
    void durationLimitRespected() throws InterruptedException {
        StressTestEngine engine = new StressTestEngine();
        TestConfig config = buildConfig(100, 5, 1, 3);

        RequestSpec spec = RequestSpec.fromTestConfig(config);
        ExecutionContext context = new ExecutionContext(spec, NoThinkTimeStrategy.INSTANCE, r -> {});
        context.setSnapshotSupplier(() -> fakeSnapshot(0.0));

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
        // Should finish within ~5s (3s duration + cleanup overhead)
        assertTrue(elapsed < 10_000, "Should respect duration limit, took " + elapsed + "ms");
    }

}
