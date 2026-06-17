package com.stormapi.engine.metrics;

import org.HdrHistogram.Histogram;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Wraps HdrHistogram to provide accurate latency percentile computation.
 *
 * Records latency in microseconds (nanos / 1000) for 0.001ms resolution.
 * Reports all values in milliseconds for display consistency.
 *
 * Thread-safety: all access protected by {@link ReentrantLock} (not synchronized)
 * to avoid virtual thread pinning on carrier threads.
 */
public class PercentileCalculator {

    /**
     * 1 microsecond — floor value. HTTP requests never respond faster than ~10μs.
     */
    private static final long LOWEST_DISCERNIBLE_VALUE = 1;

    /**
     * 30 seconds in microseconds — ceiling value.
     * Matches TestConfig.timeoutMs upper bound. Responses beyond this are timeouts.
     */
    private static final long HIGHEST_TRACKABLE_VALUE = 30_000_000L;

    /**
     * 3 significant digits = 0.1% precision.
     * P99 at 100ms reports as 99.9–100.1ms. Industry standard (JMeter, Gatling).
     */
    private static final int SIGNIFICANT_DIGITS = 3;

    private final Histogram histogram;
    private final ReentrantLock lock = new ReentrantLock();

    public PercentileCalculator() {
        this.histogram = new Histogram(LOWEST_DISCERNIBLE_VALUE,
                HIGHEST_TRACKABLE_VALUE, SIGNIFICANT_DIGITS);
    }

    /**
     * Records a latency measurement from a request result.
     * Converts nanoseconds to microseconds before recording.
     * Values exceeding 30s are clipped to prevent ArrayIndexOutOfBoundsException.
     *
     * @param responseTimeNanos latency in nanoseconds from System.nanoTime() delta
     */
    public void recordLatency(long responseTimeNanos) {
        long micros = Math.max(responseTimeNanos / 1000, LOWEST_DISCERNIBLE_VALUE);
        micros = Math.min(micros, HIGHEST_TRACKABLE_VALUE);

        lock.lock();
        try {
            histogram.recordValue(micros);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the latency at the given percentile in milliseconds.
     *
     * @param percentile 0.0–100.0 (e.g., 95.0 for P95)
     */
    public double getPercentileMs(double percentile) {
        lock.lock();
        try {
            if (histogram.getTotalCount() == 0) {
                return 0.0;
            }
            return histogram.getValueAtPercentile(percentile) / 1000.0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the minimum recorded latency in milliseconds.
     */
    public double getMinMs() {
        lock.lock();
        try {
            if (histogram.getTotalCount() == 0) {
                return 0.0;
            }
            return histogram.getMinValue() / 1000.0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the maximum recorded latency in milliseconds.
     */
    public double getMaxMs() {
        lock.lock();
        try {
            if (histogram.getTotalCount() == 0) {
                return 0.0;
            }
            return histogram.getMaxValue() / 1000.0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the mean latency in milliseconds.
     */
    public double getMeanMs() {
        lock.lock();
        try {
            if (histogram.getTotalCount() == 0) {
                return 0.0;
            }
            return histogram.getMean() / 1000.0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns total number of recorded latency values.
     */
    public long getTotalCount() {
        lock.lock();
        try {
            return histogram.getTotalCount();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets all recorded data. Used between test re-runs.
     */
    public void reset() {
        lock.lock();
        try {
            histogram.reset();
        } finally {
            lock.unlock();
        }
    }

}
