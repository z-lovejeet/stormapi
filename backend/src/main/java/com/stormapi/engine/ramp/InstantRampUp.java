package com.stormapi.engine.ramp;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.user.VirtualUserSimulator;

import java.util.function.Consumer;

/**
 * All virtual users start simultaneously. No delay between spawns.
 * Used when {@code rampUpSeconds == 0}.
 *
 * This is the simplest strategy — creates a wall of concurrent connections
 * that hits the target all at once. Useful for testing sudden load scenarios
 * or when ramp-up behavior is not important.
 */
public final class InstantRampUp implements RampUpStrategy {

    @Override
    public void execute(int totalUsers, ExecutionContext context,
                        HttpRequestExecutor executor,
                        Consumer<Thread> threadCollector) {
        for (int i = 0; i < totalUsers; i++) {
            Thread userThread = Thread.ofVirtual()
                    .name("storm-user-", i)
                    .start(new VirtualUserSimulator(context, executor));
            threadCollector.accept(userThread);
        }
    }

}
