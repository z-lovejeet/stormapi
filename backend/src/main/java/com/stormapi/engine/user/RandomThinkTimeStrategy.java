package com.stormapi.engine.user;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random delay within configurable bounds. Models realistic user behavior
 * where pause times vary between interactions.
 *
 * Uses {@link ThreadLocalRandom} for zero-contention random number generation
 * across thousands of concurrent virtual threads.
 *
 * @param minDelayMs minimum delay in milliseconds (inclusive, >= 0)
 * @param maxDelayMs maximum delay in milliseconds (inclusive, >= minDelayMs)
 */
public record RandomThinkTimeStrategy(long minDelayMs, long maxDelayMs) implements ThinkTimeStrategy {

    /**
     * Compact constructor — validates min <= max and both non-negative.
     */
    public RandomThinkTimeStrategy {
        if (minDelayMs < 0) {
            throw new IllegalArgumentException(
                    "Min delay must be non-negative, got: " + minDelayMs);
        }
        if (maxDelayMs < minDelayMs) {
            throw new IllegalArgumentException(
                    "Max delay (" + maxDelayMs + ") must be >= min delay (" + minDelayMs + ")");
        }
    }

    @Override
    public void apply() throws InterruptedException {
        long delay = getDelayMs();
        if (delay > 0) {
            Thread.sleep(Duration.ofMillis(delay));
        }
    }

    @Override
    public long getDelayMs() {
        if (minDelayMs == maxDelayMs) {
            return minDelayMs;
        }
        return ThreadLocalRandom.current().nextLong(minDelayMs, maxDelayMs + 1);
    }

}
