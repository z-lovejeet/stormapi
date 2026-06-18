package com.stormapi.engine.stress;

import com.stormapi.engine.AbstractTestEngine;
import com.stormapi.engine.analysis.EngineAnalysisResult;
import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpClientFactory;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.metrics.LiveMetricsSnapshot;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Stress Test Engine — stepwise load increase to find degradation points.
 *
 * Algorithm:
 * 1. Start with stepSize users
 * 2. Add stepSize users every stepDurationSeconds
 * 3. Monitor error rate at each step
 * 4. Stop when: maxUsers reached, error rate > 50%, duration expires, or manual stop
 *
 * Uses virtualUsers as the ceiling (max users to reach).
 * Uses stepSize for batch increments.
 * Uses stepDurationSeconds as the sustain time per step.
 *
 * Metrics collection is handled by the orchestrator's snapshot timer.
 * This engine only controls user count and monitors degradation.
 */
public class StressTestEngine extends AbstractTestEngine {

    private static final double ERROR_RATE_THRESHOLD = 50.0;

    private HttpClient httpClient;

    @Override
    public TestType getSupportedType() {
        return TestType.STRESS;
    }

    @Override
    protected void doExecute(ExecutionContext context, TestConfig config) throws InterruptedException {
        // 1. Create HTTP client
        httpClient = HttpClientFactory.create(Duration.ofMillis(config.getTimeoutMs()));
        HttpRequestExecutor executor = new HttpRequestExecutor(httpClient);

        int stepSize = (config.getStepSize() != null && config.getStepSize() > 0)
                ? config.getStepSize() : 10;
        int stepDuration = (config.getStepDurationSeconds() != null && config.getStepDurationSeconds() > 0)
                ? config.getStepDurationSeconds() : 30;
        int maxUsers = config.getVirtualUsers();
        int currentUsers = 0;
        long startTime = System.currentTimeMillis();

        // 2. Step-wise ramp-up
        while (!stopped && currentUsers < maxUsers) {
            // Check duration limit
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= config.getDurationSeconds() * 1000L) break;

            // Add users for this step
            int usersToAdd = Math.min(stepSize, maxUsers - currentUsers);
            for (int i = 0; i < usersToAdd; i++) {
                spawnUser(currentUsers + i, context, executor);
            }
            currentUsers += usersToAdd;

            // Sustain this step
            sleepInterruptibly(stepDuration * 1000L);

            // Check degradation
            if (!stopped && context.getSnapshotSupplier() != null) {
                LiveMetricsSnapshot snapshot = context.getSnapshotSupplier().get();
                if (snapshot.errorRate() > ERROR_RATE_THRESHOLD) {
                    break; // system is degraded
                }
            }
        }

        // 3. Wait for remaining duration (if we stopped stepping early due to maxUsers)
        long remainingMs = (config.getDurationSeconds() * 1000L)
                - (System.currentTimeMillis() - startTime);
        if (remainingMs > 0 && !stopped) {
            sleepInterruptibly(remainingMs);
        }

        // 4. Signal stop and wait
        context.stop();
        awaitAllUsers(Duration.ofSeconds(10));
    }

    @Override
    protected void onAfterExecute(ExecutionContext context) {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

}
