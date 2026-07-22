package com.stormapi.engine.spike;

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
 * Spike Test Engine — simulates a sudden traffic burst and measures recovery.
 *
 * Lifecycle:
 * 1. Warm-up: run base users for 25% of duration (min 5s, max 30s)
 * 2. Spike: instantly add spikeUsers, sustain for 33% of duration
 * 3. Recovery: remove spike users, monitor until response time returns to baseline
 *
 * Recovery time = how long until avgResponseTimeMs returns to within 10% of baseline.
 * Reports -1 if the system never recovers within the remaining duration.
 *
 * Uses config.spikeUsers for the burst count.
 * Uses config.virtualUsers for the base load.
 */
public class SpikeTestEngine extends AbstractTestEngine {

    private static final double RECOVERY_TOLERANCE = 1.10; // within 10% of baseline
    private static final long RECOVERY_POLL_MS = 500;

    private HttpClient httpClient;

    @Override
    public TestType getSupportedType() {
        return TestType.SPIKE;
    }

    @Override
    protected void doExecute(ExecutionContext context, TestConfig config) throws InterruptedException {
        httpClient = HttpClientFactory.create(Duration.ofMillis(config.getTimeoutMs()));
        HttpRequestExecutor executor = new HttpRequestExecutor(httpClient);

        int baseUsers = config.getVirtualUsers();
        int spikeUsers = (config.getSpikeUsers() != null && config.getSpikeUsers() > 0)
                ? config.getSpikeUsers() : baseUsers * 3; // default: 3x base
        int totalDuration = config.getDurationSeconds();

        // Phase timing
        int warmupSeconds = Math.max(1, Math.min(30, totalDuration / 4));
        int spikeDuration = totalDuration / 3;
        int recoveryDuration = totalDuration - warmupSeconds - spikeDuration;

        // ── Phase 1: Warm-up (base load) ──
        for (int i = 0; i < baseUsers; i++) {
            spawnUser(i, context, executor);
        }
        sleepInterruptibly(warmupSeconds * 1000L);
        if (stopped) return;

        // Capture baseline metrics
        double baselineAvgMs = 100.0; // default if no snapshot available
        if (context.getSnapshotSupplier() != null) {
            LiveMetricsSnapshot baseline = context.getSnapshotSupplier().get();
            if (baseline.totalRequests() > 0) {
                baselineAvgMs = baseline.avgResponseTimeMs();
            }
        }

        // ── Phase 2: Spike (instant burst) ──
        for (int i = 0; i < spikeUsers; i++) {
            spawnUser(baseUsers + i, context, executor);
        }
        sleepInterruptibly(spikeDuration * 1000L);
        if (stopped) {
            context.setAnalysisResult(EngineAnalysisResult.spike(-1));
            context.stop();
            awaitAllUsers(Duration.ofSeconds(10));
            return;
        }

        // ── Phase 3: Recovery (drop spike users) ──
        removeUsers(spikeUsers);

        long recoveryStartNanos = System.nanoTime();
        long recoveryTimeMs = -1; // -1 = never recovered

        long recoveryDeadlineMs = recoveryDuration * 1000L;
        long elapsedRecoveryMs = 0;

        while (!stopped && elapsedRecoveryMs < recoveryDeadlineMs) {
            sleepInterruptibly(RECOVERY_POLL_MS);
            elapsedRecoveryMs = (System.nanoTime() - recoveryStartNanos) / 1_000_000;

            if (context.getSnapshotSupplier() != null) {
                LiveMetricsSnapshot snapshot = context.getSnapshotSupplier().get();
                if (snapshot.avgResponseTimeMs() <= baselineAvgMs * RECOVERY_TOLERANCE) {
                    recoveryTimeMs = elapsedRecoveryMs;
                    break;
                }
            }
        }

        // Wait out remaining recovery time for steady-state observation
        long remainingRecoveryMs = recoveryDeadlineMs - elapsedRecoveryMs;
        if (remainingRecoveryMs > 0 && !stopped) {
            sleepInterruptibly(remainingRecoveryMs);
        }

        // Store result
        context.setAnalysisResult(EngineAnalysisResult.spike(recoveryTimeMs));

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
