package com.stormapi.engine.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Tracks the distribution of HTTP status codes across all requests.
 *
 * Uses {@link ConcurrentHashMap} with {@link LongAdder} values for
 * lock-free, high-concurrency recording. Typical test produces only
 * 3-5 distinct status codes, so map size is negligible.
 *
 * Connection failures (statusCode == 0) are skipped — they are already
 * counted in MetricsCollector.failureCount. This distribution should
 * only contain real HTTP responses.
 */
public class StatusCodeTracker {

    private final ConcurrentHashMap<Integer, LongAdder> statusCounts = new ConcurrentHashMap<>();

    /**
     * Records a status code occurrence. Skips statusCode == 0 (connection failures).
     */
    public void record(int statusCode) {
        if (statusCode == 0) {
            return;
        }
        statusCounts.computeIfAbsent(statusCode, k -> new LongAdder()).increment();
    }

    /**
     * Returns the count for a specific status code.
     */
    public long getCount(int statusCode) {
        LongAdder adder = statusCounts.get(statusCode);
        return (adder != null) ? adder.sum() : 0;
    }

    /**
     * Returns an immutable snapshot of the full status code distribution.
     */
    public Map<Integer, Long> getDistribution() {
        return Map.copyOf(
                statusCounts.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()))
        );
    }

    /**
     * Resets all tracked status codes. Used between test re-runs.
     */
    public void reset() {
        statusCounts.clear();
    }

}
