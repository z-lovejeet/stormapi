package com.stormapi.engine.ramp;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;

import java.util.function.Consumer;

/**
 * Defines how virtual users are spawned over time during a test.
 *
 * Sealed interface — only three strategies exist:
 * - {@link InstantRampUp}: all users start simultaneously
 * - {@link LinearRampUp}: users added evenly over the ramp-up period
 * - {@link StepRampUp}: users added in batches at fixed intervals
 *
 * The strategy creates and starts virtual threads, passing each to
 * the caller via {@code threadCollector} for lifecycle tracking.
 */
public sealed interface RampUpStrategy
        permits InstantRampUp, LinearRampUp, StepRampUp {

    /**
     * Spawns virtual users according to the strategy's timing rules.
     *
     * @param totalUsers      number of virtual users to spawn
     * @param context         shared execution context
     * @param executor        HTTP request executor (shared across users)
     * @param threadCollector callback to collect spawned threads for lifecycle management
     * @throws InterruptedException if interrupted during sleep intervals
     */
    void execute(int totalUsers, ExecutionContext context,
                 HttpRequestExecutor executor,
                 Consumer<Thread> threadCollector) throws InterruptedException;

    /**
     * Factory method — determines the correct strategy from test configuration.
     *
     * Logic:
     * - rampUpSeconds == 0 → InstantRampUp
     * - testType is STRESS/BREAKPOINT/SCALABILITY with stepSize > 0 → StepRampUp
     * - otherwise → LinearRampUp
     */
    static RampUpStrategy fromConfig(TestConfig config) {
        if (config.getRampUpSeconds() <= 0) {
            return new InstantRampUp();
        }

        boolean isSteppedType = config.getTestType() == TestType.STRESS
                || config.getTestType() == TestType.BREAKPOINT
                || config.getTestType() == TestType.SCALABILITY;

        if (isSteppedType && config.getStepSize() != null && config.getStepSize() > 0) {
            int stepDuration = (config.getStepDurationSeconds() != null && config.getStepDurationSeconds() > 0)
                    ? config.getStepDurationSeconds()
                    : config.getRampUpSeconds();
            return new StepRampUp(config.getStepSize(), stepDuration);
        }

        return new LinearRampUp(config.getRampUpSeconds());
    }

}
