package com.stormapi.engine.metrics;

import com.stormapi.engine.http.RequestResult;

import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Central metrics aggregation hub for a single test run.
 *
 * Receives every {@link RequestResult} from every virtual user thread,
 * updates all internal trackers, and produces {@link LiveMetricsSnapshot}
 * on demand. This is the most concurrency-critical class in the application.
 *
 * Concurrency model:
 * - Hot path ({@link #recordResult}): called per request by 10,000+ virtual threads.
 *   Uses {@link LongAdder} for lock-free counting and brief {@link java.util.concurrent.locks.ReentrantLock}
 *   in {@link PercentileCalculator} for histogram updates.
 * - Cool path ({@link #snapshot}): called ~1/s by the sampling timer.
 *   Reads {@link LongAdder#sum()} (eventually consistent) and histogram percentiles.
 *
 * NOT a Spring bean — instantiated per test run by TestOrchestrator (Phase 6).
 */
public class MetricsCollector {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final LongAdder totalBytes = new LongAdder();
    private final LongAdder responseTimeNanosSum = new LongAdder();

    private final PercentileCalculator percentileCalculator;
    private final StatusCodeTracker statusCodeTracker;
    private final ThroughputTracker throughputTracker;
    private final Supplier<Integer> activeUsersSupplier;

    /**
     * Creates a MetricsCollector for a single test run.
     *
     * @param activeUsersSupplier reads active user count from ExecutionContext
     *                            (e.g., {@code () -> executionContext.getActiveUsers()}).
     *                            Avoids duplicating the counter — single source of truth.
     */
    public MetricsCollector(Supplier<Integer> activeUsersSupplier) {
        this.activeUsersSupplier = activeUsersSupplier;
        this.percentileCalculator = new PercentileCalculator();
        this.statusCodeTracker = new StatusCodeTracker();
        this.throughputTracker = new ThroughputTracker();
    }

    /**
     * Records a single request result. Called by every virtual user after
     * each HTTP request execution. This is the HOT PATH.
     *
     * Operations 1-4 and 7 are lock-free (LongAdder).
     * Operation 5 briefly acquires a ReentrantLock (PercentileCalculator).
     * Operation 6 is lock-free (ConcurrentHashMap + LongAdder).
     *
     * @param result the request result to record
     */
    public void recordResult(RequestResult result) {
        // 1. Count total requests
        totalRequests.increment();

        // 2. Count successes/failures
        if (result.success()) {
            successCount.increment();
        } else {
            failureCount.increment();
        }

        // 3. Accumulate response body size
        totalBytes.add(result.responseBodySize());

        // 4. Accumulate response time for average calculation
        responseTimeNanosSum.add(result.responseTimeNanos());

        // 5. Record latency into HdrHistogram (brief ReentrantLock)
        percentileCalculator.recordLatency(result.responseTimeNanos());

        // 6. Track status code distribution (lock-free)
        if (result.statusCode() > 0) {
            statusCodeTracker.record(result.statusCode());
        }

        // 7. Record for throughput calculation (lock-free)
        throughputTracker.record();
    }

    /**
     * Produces an immutable snapshot of all current metrics.
     * Called ~1/s by the sampling timer. The COOL PATH.
     *
     * Snapshot is eventually consistent — during the ~1-5μs read window,
     * concurrent writers may update some counters. This means success + failure
     * might differ from total by 1-2 out of millions. Acceptable for metrics.
     *
     * @return immutable point-in-time metrics snapshot
     */
    public LiveMetricsSnapshot snapshot() {
        long total = totalRequests.sum();
        long success = successCount.sum();
        long failure = failureCount.sum();
        long bytes = totalBytes.sum();

        double avgMs = (total > 0)
                ? (responseTimeNanosSum.sum() / (double) total) / 1_000_000.0
                : 0.0;

        double errorRate = (total > 0) ? (failure * 100.0 / total) : 0.0;

        return new LiveMetricsSnapshot(
                total,
                success,
                failure,
                avgMs,
                percentileCalculator.getMinMs(),
                percentileCalculator.getMaxMs(),
                percentileCalculator.getPercentileMs(50.0),
                percentileCalculator.getPercentileMs(75.0),
                percentileCalculator.getPercentileMs(90.0),
                percentileCalculator.getPercentileMs(95.0),
                percentileCalculator.getPercentileMs(99.0),
                throughputTracker.getCurrentRps(),
                errorRate,
                activeUsersSupplier.get(),
                bytes,
                statusCodeTracker.getDistribution(),
                Instant.now()
        );
    }

    /**
     * Resets all metrics to initial state. Used between test re-runs.
     */
    public void reset() {
        totalRequests.reset();
        successCount.reset();
        failureCount.reset();
        totalBytes.reset();
        responseTimeNanosSum.reset();
        percentileCalculator.reset();
        statusCodeTracker.reset();
        throughputTracker.reset();
    }

}
