package com.stormapi.engine.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * Calculates real-time requests-per-second (RPS) using a sliding-window
 * ring buffer of 1-second buckets.
 *
 * Reports *current* throughput from the most recently completed second,
 * NOT overall average (totalRequests / elapsed). This reveals throughput
 * degradation during tests that an overall average would hide.
 *
 * Thread-safety: each bucket is a {@link LongAdder} — lock-free writes.
 * Writers only touch the current bucket; the reader reads the previous bucket.
 */
public class ThroughputTracker {

    private static final int WINDOW_SIZE = 10;

    private final LongAdder[] buckets;
    private volatile long lastSecond = -1;

    public ThroughputTracker() {
        this.buckets = new LongAdder[WINDOW_SIZE];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            buckets[i] = new LongAdder();
        }
    }

    /**
     * Records a request in the current 1-second bucket.
     * If the second has changed since the last call, the new bucket is reset first.
     */
    public void record() {
        long currentSecond = System.nanoTime() / 1_000_000_000L;
        int index = (int) (currentSecond % WINDOW_SIZE);

        // If we've moved to a new second, reset the bucket (it has stale data from WINDOW_SIZE seconds ago)
        if (currentSecond != lastSecond) {
            buckets[index].reset();
            lastSecond = currentSecond;
        }

        buckets[index].increment();
    }

    /**
     * Returns the RPS from the most recently completed 1-second window.
     * Reading the *previous* second ensures the count is final (no more writes).
     */
    public double getCurrentRps() {
        long currentSecond = System.nanoTime() / 1_000_000_000L;
        long previousSecond = currentSecond - 1;
        int index = (int) (previousSecond % WINDOW_SIZE);

        // Guard: if the previous bucket is stale (no activity for > WINDOW_SIZE seconds)
        if (currentSecond - previousSecond > WINDOW_SIZE) {
            return 0.0;
        }

        return buckets[index].sum();
    }

    /**
     * Resets all buckets. Used between test re-runs.
     */
    public void reset() {
        for (LongAdder bucket : buckets) {
            bucket.reset();
        }
        lastSecond = -1;
    }

}
