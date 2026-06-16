package com.stormapi.engine.user;

import java.time.Duration;

/**
 * Fixed delay between requests. Models users with predictable,
 * constant pause between interactions.
 *
 * @param delayMs the fixed delay in milliseconds (must be >= 0)
 */
public record ConstantThinkTimeStrategy(long delayMs) implements ThinkTimeStrategy {

    /**
     * Compact constructor — validates delay is non-negative.
     */
    public ConstantThinkTimeStrategy {
        if (delayMs < 0) {
            throw new IllegalArgumentException("Delay must be non-negative, got: " + delayMs);
        }
    }

    @Override
    public void apply() throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(Duration.ofMillis(delayMs));
        }
    }

    @Override
    public long getDelayMs() {
        return delayMs;
    }

}
