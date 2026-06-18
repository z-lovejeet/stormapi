package com.stormapi.engine.scalability;

import com.stormapi.engine.AbstractTestEngine;
import com.stormapi.engine.analysis.EngineAnalysisResult;
import com.stormapi.engine.analysis.EngineAnalysisResult.ScalabilityPoint;
import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpClientFactory;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.metrics.LiveMetricsSnapshot;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Scalability Test Engine — measures throughput at predefined user steps
 * to generate a scalability curve for capacity planning.
 *
 * Algorithm:
 * 1. For each step: add stepSize users
 * 2. Wait 5 seconds for stabilization (new threads warm up)
 * 3. Capture pre-measurement snapshot
 * 4. Sustain remaining step duration
 * 5. Capture post-measurement snapshot
 * 6. Compute step-specific throughput from delta
 * 7. Record ScalabilityPoint(users, throughput, latency, errorRate)
 *
 * The 5-second stabilization window prevents newly spawned threads
 * from polluting the measurements with startup transients.
 *
 * Output: List<ScalabilityPoint> — the scalability curve.
 * The inflection point where throughput plateaus indicates the system's
 * scalability limit.
 */
public class ScalabilityTestEngine extends AbstractTestEngine {

    private static final int STABILIZATION_SECONDS = 5;

    private HttpClient httpClient;

    @Override
    public TestType getSupportedType() {
        return TestType.SCALABILITY;
    }

    @Override
    protected void doExecute(ExecutionContext context, TestConfig config) throws InterruptedException {
        httpClient = HttpClientFactory.create(Duration.ofMillis(config.getTimeoutMs()));
        HttpRequestExecutor executor = new HttpRequestExecutor(httpClient);

        int stepSize = (config.getStepSize() != null && config.getStepSize() > 0)
                ? config.getStepSize() : 10;
        int stepDuration = (config.getStepDurationSeconds() != null && config.getStepDurationSeconds() > 0)
                ? config.getStepDurationSeconds() : 30;
        int maxUsers = config.getVirtualUsers();
        int currentUsers = 0;

        List<ScalabilityPoint> curve = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        while (!stopped && currentUsers < maxUsers) {
            // Check duration limit
            if (isTimedOut(startTime, config.getDurationSeconds())) break;

            // Add users for this step
            int usersToAdd = Math.min(stepSize, maxUsers - currentUsers);
            for (int i = 0; i < usersToAdd; i++) {
                spawnUser(currentUsers + i, context, executor);
            }
            currentUsers += usersToAdd;

            // Stabilization window
            int stabilizeTime = Math.min(STABILIZATION_SECONDS, stepDuration);
            sleepInterruptibly(stabilizeTime * 1000L);
            if (stopped) break;

            // Pre-measurement snapshot
            LiveMetricsSnapshot preSnapshot = null;
            if (context.getSnapshotSupplier() != null) {
                preSnapshot = context.getSnapshotSupplier().get();
            }

            // Measurement window
            int measureDuration = Math.max(1, stepDuration - stabilizeTime);
            sleepInterruptibly(measureDuration * 1000L);
            if (stopped && preSnapshot == null) break;

            // Post-measurement snapshot
            if (context.getSnapshotSupplier() != null) {
                LiveMetricsSnapshot postSnapshot = context.getSnapshotSupplier().get();

                // Calculate step-specific throughput from delta
                double stepThroughput;
                if (preSnapshot != null) {
                    long deltaRequests = postSnapshot.totalRequests() - preSnapshot.totalRequests();
                    stepThroughput = deltaRequests / (double) measureDuration;
                } else {
                    stepThroughput = postSnapshot.throughputRps();
                }

                curve.add(new ScalabilityPoint(
                        currentUsers,
                        stepThroughput,
                        postSnapshot.avgResponseTimeMs(),
                        postSnapshot.errorRate()
                ));
            }
        }

        // Store scalability curve
        context.setAnalysisResult(EngineAnalysisResult.scalability(curve));

        context.stop();
        awaitAllUsers(Duration.ofSeconds(10));
    }

    private boolean isTimedOut(long startTime, int durationSeconds) {
        return (System.currentTimeMillis() - startTime) >= durationSeconds * 1000L;
    }

    @Override
    protected void onAfterExecute(ExecutionContext context) {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

}
