package com.stormapi.engine.ramp;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.user.VirtualUserSimulator;

import java.util.function.Consumer;

/**
 * Adds virtual users evenly over the ramp-up period.
 *
 * Algorithm: intervalMs = (rampUpSeconds * 1000) / totalUsers
 * Example: 100 users, 10s ramp-up → 1 user every 100ms
 *
 * If the test is stopped or context is no longer running during ramp-up,
 * the remaining users are skipped (partial ramp-up).
 */
public final class LinearRampUp implements RampUpStrategy {

    private final int rampUpSeconds;

    public LinearRampUp(int rampUpSeconds) {
        this.rampUpSeconds = rampUpSeconds;
    }

    @Override
    public void execute(int totalUsers, ExecutionContext context,
                        HttpRequestExecutor executor,
                        Consumer<Thread> threadCollector) throws InterruptedException {
        if (totalUsers <= 0) return;

        long intervalMs = (rampUpSeconds * 1000L) / totalUsers;
        // Ensure at least 1ms interval to prevent busy-spin
        intervalMs = Math.max(intervalMs, 1);

        for (int i = 0; i < totalUsers; i++) {
            if (!context.isRunning()) break;

            Thread userThread = Thread.ofVirtual()
                    .name("storm-user-", i)
                    .start(new VirtualUserSimulator(context, executor));
            threadCollector.accept(userThread);

            // Don't sleep after the last user
            if (i < totalUsers - 1) {
                Thread.sleep(intervalMs);
            }
        }
    }

    public int getRampUpSeconds() {
        return rampUpSeconds;
    }

}
