package com.stormapi.engine.ramp;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpClientFactory;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.http.RequestSpec;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RampUpStrategy Tests")
class RampUpStrategyTest {

    private ExecutionContext context;
    private HttpRequestExecutor executor;
    private List<Thread> collectedThreads;

    @BeforeEach
    void setUp() {
        RequestSpec dummySpec = new RequestSpec(
                "http://localhost:1/noop", "GET", Map.of(), null, java.time.Duration.ofMillis(100));
        context = new ExecutionContext(dummySpec, com.stormapi.engine.user.NoThinkTimeStrategy.INSTANCE, result -> {});
        context.start();
        executor = new HttpRequestExecutor(HttpClientFactory.createDefault());
        collectedThreads = Collections.synchronizedList(new ArrayList<>());
    }

    // ── InstantRampUp ──────────────────────────────────────────────

    @Test
    @DisplayName("InstantRampUp — all users spawned immediately")
    void instantRampUp_allUsersSpawnedImmediately() throws InterruptedException {
        InstantRampUp rampUp = new InstantRampUp();
        long start = System.currentTimeMillis();

        rampUp.execute(50, context, executor, collectedThreads::add);

        long elapsed = System.currentTimeMillis() - start;
        assertEquals(50, collectedThreads.size(), "All 50 users should be spawned");
        assertTrue(elapsed < 500, "Should complete in < 500ms, took: " + elapsed);

        // Cleanup
        context.stop();
        for (Thread t : collectedThreads) t.join(5000);
    }

    @Test
    @DisplayName("InstantRampUp — zero users is no-op")
    void instantRampUp_zeroUsers() throws InterruptedException {
        new InstantRampUp().execute(0, context, executor, collectedThreads::add);
        assertTrue(collectedThreads.isEmpty());
        context.stop();
    }

    // ── LinearRampUp ───────────────────────────────────────────────

    @Test
    @DisplayName("LinearRampUp — users spawned gradually over ramp-up period")
    void linearRampUp_usersSpawnedGradually() throws InterruptedException {
        LinearRampUp rampUp = new LinearRampUp(2); // 2 seconds for 10 users
        long start = System.currentTimeMillis();

        rampUp.execute(10, context, executor, collectedThreads::add);

        long elapsed = System.currentTimeMillis() - start;
        assertEquals(10, collectedThreads.size(), "All 10 users should be spawned");
        assertTrue(elapsed >= 1500 && elapsed <= 3000,
                "Should take ~2 seconds, took: " + elapsed + "ms");

        context.stop();
        for (Thread t : collectedThreads) t.join(5000);
    }

    @Test
    @DisplayName("LinearRampUp — stops when context is stopped mid-ramp")
    void linearRampUp_stopsWhenContextStopped() throws InterruptedException {
        LinearRampUp rampUp = new LinearRampUp(10); // 10 seconds for 100 users

        // Stop context after 500ms
        Thread.ofVirtual().start(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            context.stop();
        });

        rampUp.execute(100, context, executor, collectedThreads::add);

        assertTrue(collectedThreads.size() < 100,
                "Should have fewer than 100 users, got: " + collectedThreads.size());

        for (Thread t : collectedThreads) t.join(5000);
    }

    // ── StepRampUp ─────────────────────────────────────────────────

    @Test
    @DisplayName("StepRampUp — users spawned in batches")
    void stepRampUp_usersSpawnedInBatches() throws InterruptedException {
        StepRampUp rampUp = new StepRampUp(5, 1); // 5 users per step, 1s between steps
        long start = System.currentTimeMillis();

        rampUp.execute(15, context, executor, collectedThreads::add);

        long elapsed = System.currentTimeMillis() - start;
        assertEquals(15, collectedThreads.size(), "All 15 users should be spawned");
        // 3 steps × 1s between = ~2s total (no sleep after last step)
        assertTrue(elapsed >= 1500 && elapsed <= 3500,
                "Should take ~2s, took: " + elapsed + "ms");

        context.stop();
        for (Thread t : collectedThreads) t.join(5000);
    }

    @Test
    @DisplayName("StepRampUp — handles uneven division (last batch gets remainder)")
    void stepRampUp_handlesUnevenDivision() throws InterruptedException {
        StepRampUp rampUp = new StepRampUp(4, 1); // 4 users per step

        rampUp.execute(10, context, executor, collectedThreads::add);

        assertEquals(10, collectedThreads.size(),
                "All 10 users should be spawned (4 + 4 + 2)");

        context.stop();
        for (Thread t : collectedThreads) t.join(5000);
    }

    // ── Factory (fromConfig) ───────────────────────────────────────

    @Test
    @DisplayName("fromConfig — rampUpSeconds=0 returns InstantRampUp")
    void fromConfig_rampUpZero_returnsInstant() {
        TestConfig config = TestConfig.builder()
                .name("test").targetUrl("http://test.com").httpMethod(com.stormapi.test.model.HttpMethod.GET)
                .testType(TestType.LOAD).virtualUsers(10).durationSeconds(10)
                .rampUpSeconds(0).build();

        assertInstanceOf(InstantRampUp.class, RampUpStrategy.fromConfig(config));
    }

    @Test
    @DisplayName("fromConfig — rampUpSeconds > 0 returns LinearRampUp for LOAD type")
    void fromConfig_rampUpPositive_returnsLinear() {
        TestConfig config = TestConfig.builder()
                .name("test").targetUrl("http://test.com").httpMethod(com.stormapi.test.model.HttpMethod.GET)
                .testType(TestType.LOAD).virtualUsers(10).durationSeconds(10)
                .rampUpSeconds(5).build();

        assertInstanceOf(LinearRampUp.class, RampUpStrategy.fromConfig(config));
    }

    @Test
    @DisplayName("fromConfig — STRESS type with stepSize returns StepRampUp")
    void fromConfig_stressWithStep_returnsStep() {
        TestConfig config = TestConfig.builder()
                .name("test").targetUrl("http://test.com").httpMethod(com.stormapi.test.model.HttpMethod.GET)
                .testType(TestType.STRESS).virtualUsers(100).durationSeconds(30)
                .rampUpSeconds(10).stepSize(20).stepDurationSeconds(5).build();

        assertInstanceOf(StepRampUp.class, RampUpStrategy.fromConfig(config));
    }

}
