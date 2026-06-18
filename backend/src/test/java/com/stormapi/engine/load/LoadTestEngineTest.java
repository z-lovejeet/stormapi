package com.stormapi.engine.load;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.RequestResult;
import com.stormapi.engine.metrics.MetricsCollector;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoadTestEngine Tests")
class LoadTestEngineTest {

    private TestConfig buildConfig(int users, int durationSec, int rampUpSec) {
        return TestConfig.builder()
                .name("test")
                .targetUrl("http://localhost:9999/test") // unreachable, will fail fast
                .httpMethod(HttpMethod.GET)
                .testType(TestType.LOAD)
                .virtualUsers(users)
                .durationSeconds(durationSec)
                .rampUpSeconds(rampUpSec)
                .timeoutMs(500)
                .thinkTimeMs(0)
                .build();
    }

    @Test
    @DisplayName("returns LOAD as supported type")
    void getSupportedType_returnsLoad() {
        assertEquals(TestType.LOAD, new LoadTestEngine().getSupportedType());
    }

    @Test
    @Timeout(15)
    @DisplayName("executes for approximately the configured duration")
    void execute_runsForConfiguredDuration() throws InterruptedException {
        TestConfig config = buildConfig(5, 3, 0); // 5 users, 3s, instant ramp-up
        AtomicInteger requestCount = new AtomicInteger(0);

        MetricsCollector mc = new MetricsCollector(() -> 0);
        ExecutionContext context = new ExecutionContext(
                com.stormapi.engine.http.RequestSpec.fromTestConfig(config),
                com.stormapi.engine.user.ThinkTimeStrategy.fromConfig(config),
                result -> {
                    mc.recordResult(result);
                    requestCount.incrementAndGet();
                }
        );
        context.setMaxRetries(0);

        LoadTestEngine engine = new LoadTestEngine();

        long start = System.currentTimeMillis();
        context.start();
        engine.execute(context, config);
        long elapsed = System.currentTimeMillis() - start;

        // Should run for ~3 seconds (±1.5s tolerance for CI)
        assertTrue(elapsed >= 2000 && elapsed <= 5000,
                "Should run ~3 seconds, took: " + elapsed + "ms");

        // Users should have exited
        assertEquals(0, context.getActiveUsers(), "All users should have exited");
        assertTrue(requestCount.get() > 0, "Should have sent at least some requests");
    }

    @Test
    @Timeout(10)
    @DisplayName("stop() terminates test early")
    void stop_terminatesEarly() throws InterruptedException {
        TestConfig config = buildConfig(5, 30, 0); // 30s duration — would time out if stop() doesn't work

        ExecutionContext context = new ExecutionContext(
                com.stormapi.engine.http.RequestSpec.fromTestConfig(config),
                com.stormapi.engine.user.ThinkTimeStrategy.fromConfig(config),
                result -> {}
        );

        LoadTestEngine engine = new LoadTestEngine();

        // Stop after 1 second
        Thread.ofVirtual().start(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            engine.stop();
            context.stop();
        });

        long start = System.currentTimeMillis();
        context.start();
        engine.execute(context, config);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 15000, "Should stop well before 30s, took: " + elapsed + "ms");
        assertEquals(0, context.getActiveUsers(), "All users should have exited");
    }

    @Test
    @Timeout(15)
    @DisplayName("uses LinearRampUp when rampUpSeconds > 0")
    void execute_usesLinearRampUp() throws InterruptedException {
        TestConfig config = buildConfig(10, 4, 2); // 10 users, 4s total, 2s ramp-up
        AtomicLong maxActiveUsers = new AtomicLong(0);

        ExecutionContext context = new ExecutionContext(
                com.stormapi.engine.http.RequestSpec.fromTestConfig(config),
                com.stormapi.engine.user.ThinkTimeStrategy.fromConfig(config),
                result -> {}
        );

        // Monitor active users during ramp-up
        Thread monitor = Thread.ofVirtual().start(() -> {
            while (context.isRunning() || context.getActiveUsers() > 0) {
                long current = context.getActiveUsers();
                maxActiveUsers.updateAndGet(prev -> Math.max(prev, current));
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
        });

        context.start();
        new LoadTestEngine().execute(context, config);

        monitor.interrupt();
        monitor.join(2000);

        assertEquals(0, context.getActiveUsers(), "All users should have exited");
        assertTrue(maxActiveUsers.get() > 0, "Should have had active users during test");
    }

    @Test
    @Timeout(10)
    @DisplayName("handles instant ramp-up (rampUpSeconds = 0)")
    void execute_instantRampUp() throws InterruptedException {
        TestConfig config = buildConfig(20, 2, 0); // 20 users, instant
        AtomicInteger peakUsers = new AtomicInteger(0);

        ExecutionContext context = new ExecutionContext(
                com.stormapi.engine.http.RequestSpec.fromTestConfig(config),
                com.stormapi.engine.user.ThinkTimeStrategy.fromConfig(config),
                result -> {}
        );

        Thread monitor = Thread.ofVirtual().start(() -> {
            while (context.isRunning() || context.getActiveUsers() > 0) {
                peakUsers.updateAndGet(prev -> Math.max(prev, context.getActiveUsers()));
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            }
        });

        context.start();
        new LoadTestEngine().execute(context, config);

        monitor.interrupt();
        monitor.join(2000);

        assertEquals(0, context.getActiveUsers());
        assertTrue(peakUsers.get() >= 15,
                "Peak users should be near 20 for instant ramp-up, got: " + peakUsers.get());
    }

}
