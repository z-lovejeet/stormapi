package com.stormapi.engine.user;

/**
 * Zero-delay think time. Used for maximum throughput tests
 * where virtual users fire requests as fast as possible.
 */
public record NoThinkTimeStrategy() implements ThinkTimeStrategy {

    /** Singleton instance — no state to differentiate between instances. */
    public static final NoThinkTimeStrategy INSTANCE = new NoThinkTimeStrategy();

    @Override
    public void apply() {
        // no-op — zero delay
    }

    @Override
    public long getDelayMs() {
        return 0;
    }

}
