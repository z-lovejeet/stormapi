package com.stormapi.engine.breakpoint;

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
 * Breakpoint Test Engine — finds the exact user count at which the system breaks
 * using a two-phase approach: exponential probe + binary search refinement.
 *
 * Phase 1 (Exponential Probe):
 *   Start at stepSize users, double each step until degradation is detected.
 *   This finds the upper bound in O(log n) steps.
 *
 * Phase 2 (Binary Search Refinement):
 *   Bisect between lastStable and upperBound until the gap ≤ stepSize.
 *   Each step sustains load for stepDurationSeconds to avoid false positives.
 *
 * Degradation predicate:
 *   - Error rate > 10%
 *   - P95 latency > 5x baseline
 *   - P95 latency > 10,000 ms (absolute ceiling)
 *
 * Total complexity: O(log n) vs O(n) for linear stress testing.
 * For 1000 users with step=10: ~14 steps vs ~100 steps.
 */
public class BreakpointTestEngine extends AbstractTestEngine {

    private static final double ERROR_RATE_THRESHOLD = 10.0;
    private static final double LATENCY_MULTIPLIER = 5.0;
    private static final double ABSOLUTE_LATENCY_CEILING_MS = 10_000.0;

    private HttpClient httpClient;
    private double baselineP95 = 0.0;

    @Override
    public TestType getSupportedType() {
        return TestType.BREAKPOINT;
    }

    @Override
    protected void doExecute(ExecutionContext context, TestConfig config) throws InterruptedException {
        httpClient = HttpClientFactory.create(Duration.ofMillis(config.getTimeoutMs()));
        HttpRequestExecutor executor = new HttpRequestExecutor(httpClient);

        int stepSize = (config.getStepSize() != null && config.getStepSize() > 0)
                ? config.getStepSize() : 10;
        int stepDuration = (config.getStepDurationSeconds() != null && config.getStepDurationSeconds() > 0)
                ? config.getStepDurationSeconds() : 15;
        int maxUsers = config.getVirtualUsers();

        int lastStableUsers = 0;
        int currentUsers = Math.min(stepSize, maxUsers);
        long startTime = System.currentTimeMillis();

        // ── Phase 1: Exponential Probe ──────────────────────────────
        // Spawn initial users
        spawnUsers(currentUsers, 0, context, executor);
        sleepInterruptibly(stepDuration * 1000L);

        if (stopped) {
            setResult(context, lastStableUsers);
            return;
        }

        // Capture baseline from first step
        if (context.getSnapshotSupplier() != null) {
            LiveMetricsSnapshot snap = context.getSnapshotSupplier().get();
            baselineP95 = snap.p95Ms() > 0 ? snap.p95Ms() : 100.0;
            if (!isDegraded(snap)) {
                lastStableUsers = currentUsers;
            }
        }

        // Double users until degradation or maxUsers
        while (!stopped && !isTimedOut(startTime, config.getDurationSeconds())) {
            int nextUsers = Math.min(currentUsers * 2, maxUsers);
            if (nextUsers <= currentUsers) break; // already at max

            adjustUserCount(nextUsers, currentUsers, context, executor);
            currentUsers = nextUsers;

            sleepInterruptibly(stepDuration * 1000L);
            if (stopped) break;

            if (context.getSnapshotSupplier() != null) {
                LiveMetricsSnapshot snap = context.getSnapshotSupplier().get();
                if (isDegraded(snap)) {
                    break; // found upper bound
                }
                lastStableUsers = currentUsers;
            }

            if (currentUsers >= maxUsers) break;
        }

        int upperBound = currentUsers;
        int lowerBound = lastStableUsers;

        // ── Phase 2: Binary Search Refinement ───────────────────────
        while (!stopped && (upperBound - lowerBound) > stepSize
                && !isTimedOut(startTime, config.getDurationSeconds())) {

            int midpoint = (lowerBound + upperBound) / 2;

            adjustUserCount(midpoint, userThreads.size(), context, executor);
            sleepInterruptibly(stepDuration * 1000L);
            if (stopped) break;

            if (context.getSnapshotSupplier() != null) {
                LiveMetricsSnapshot snap = context.getSnapshotSupplier().get();
                if (isDegraded(snap)) {
                    upperBound = midpoint;
                } else {
                    lowerBound = midpoint;
                    lastStableUsers = midpoint;
                }
            }
        }

        setResult(context, lastStableUsers);

        context.stop();
        awaitAllUsers(Duration.ofSeconds(10));
    }

    /**
     * Checks if the system is degraded based on current metrics.
     */
    private boolean isDegraded(LiveMetricsSnapshot snapshot) {
        if (snapshot.totalRequests() == 0) return false;

        return snapshot.errorRate() > ERROR_RATE_THRESHOLD
                || (baselineP95 > 0 && snapshot.p95Ms() > baselineP95 * LATENCY_MULTIPLIER)
                || snapshot.p95Ms() > ABSOLUTE_LATENCY_CEILING_MS;
    }

    /**
     * Adjusts the number of active users to the target count.
     * Spawns or removes users as needed.
     */
    private void adjustUserCount(int target, int current,
                                  ExecutionContext context, HttpRequestExecutor executor) {
        if (target > current) {
            spawnUsers(target - current, current, context, executor);
        } else if (target < current) {
            removeUsers(current - target);
        }
    }

    /**
     * Spawns a batch of users starting at the given offset.
     */
    private void spawnUsers(int count, int startId,
                             ExecutionContext context, HttpRequestExecutor executor) {
        for (int i = 0; i < count; i++) {
            spawnUser(startId + i, context, executor);
        }
    }

    private boolean isTimedOut(long startTime, int durationSeconds) {
        return (System.currentTimeMillis() - startTime) >= durationSeconds * 1000L;
    }

    private void setResult(ExecutionContext context, int breakpointUsers) {
        context.setAnalysisResult(EngineAnalysisResult.breakpoint(breakpointUsers));
    }

    @Override
    protected void onAfterExecute(ExecutionContext context) {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

}
