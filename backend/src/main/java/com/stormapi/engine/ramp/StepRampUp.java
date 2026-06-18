package com.stormapi.engine.ramp;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.user.VirtualUserSimulator;

import java.util.function.Consumer;

/**
 * Adds virtual users in batches at fixed intervals.
 * Used for STRESS, BREAKPOINT, and SCALABILITY test types.
 *
 * Algorithm:
 *   steps = ceil(totalUsers / stepSize)
 *   for each step:
 *     spawn min(stepSize, remaining) users
 *     sleep(stepDurationSeconds * 1000)
 *
 * Example: 100 users, stepSize=20, stepDuration=5s
 *   → 20 users at t=0, 20 at t=5s, 20 at t=10s, 20 at t=15s, 20 at t=20s
 */
public final class StepRampUp implements RampUpStrategy {

    private final int stepSize;
    private final int stepDurationSeconds;

    public StepRampUp(int stepSize, int stepDurationSeconds) {
        this.stepSize = stepSize;
        this.stepDurationSeconds = stepDurationSeconds;
    }

    @Override
    public void execute(int totalUsers, ExecutionContext context,
                        HttpRequestExecutor executor,
                        Consumer<Thread> threadCollector) throws InterruptedException {
        if (totalUsers <= 0) return;

        int spawned = 0;
        int steps = (int) Math.ceil((double) totalUsers / stepSize);

        for (int step = 0; step < steps; step++) {
            if (!context.isRunning()) break;

            int usersThisStep = Math.min(stepSize, totalUsers - spawned);

            for (int i = 0; i < usersThisStep; i++) {
                Thread userThread = Thread.ofVirtual()
                        .name("storm-user-", spawned + i)
                        .start(new VirtualUserSimulator(context, executor));
                threadCollector.accept(userThread);
            }
            spawned += usersThisStep;

            // Don't sleep after the last step
            if (step < steps - 1 && context.isRunning()) {
                Thread.sleep(stepDurationSeconds * 1000L);
            }
        }
    }

    public int getStepSize() {
        return stepSize;
    }

    public int getStepDurationSeconds() {
        return stepDurationSeconds;
    }

}
