package com.stormapi.engine.user;

import com.stormapi.test.model.TestConfig;

/**
 * Sealed interface defining how long a virtual user waits between requests.
 *
 * Sealed = all implementations known at compile time, enabling exhaustive
 * switch expressions in Phase 6+ test orchestrators.
 */
public sealed interface ThinkTimeStrategy
        permits NoThinkTimeStrategy, ConstantThinkTimeStrategy, RandomThinkTimeStrategy {

    /**
     * Blocks the current thread for the think-time duration.
     * On virtual threads, Thread.sleep() unmounts the virtual thread
     * from the carrier — no platform thread is wasted.
     */
    void apply() throws InterruptedException;

    /**
     * Returns the delay in milliseconds without sleeping.
     * Useful for logging and testing.
     */
    long getDelayMs();

    /**
     * Factory method to create the appropriate strategy from a TestConfig.
     */
    static ThinkTimeStrategy fromConfig(TestConfig config) {
        if (config.getThinkTimeMs() <= 0) {
            return NoThinkTimeStrategy.INSTANCE;
        }
        return new ConstantThinkTimeStrategy(config.getThinkTimeMs());
    }

}
