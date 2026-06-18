package com.stormapi.engine;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;

/**
 * Contract for all test engine implementations.
 *
 * Each test type (Load, Stress, Spike, Soak, Breakpoint, Scalability)
 * has a corresponding engine that defines its unique load pattern.
 *
 * The engine is instantiated per test run — it is NOT a Spring bean.
 * The {@link com.stormapi.test.service.TestOrchestrator} creates the engine,
 * runs {@link #execute} on a virtual thread, and handles cleanup.
 *
 * Thread-safety contract:
 * - {@link #execute} runs on one virtual thread (the orchestrator thread).
 * - {@link #stop} may be called from a different thread (the HTTP request thread).
 */
public interface TestEngine {

    /**
     * Returns the test type this engine supports.
     */
    TestType getSupportedType();

    /**
     * Executes the test synchronously. Blocks until the test completes,
     * is stopped, or fails. The caller (TestOrchestrator) runs this
     * on a virtual thread.
     *
     * @param context shared runtime state (running flag, result consumer, active users)
     * @param config  test configuration (virtualUsers, durationSeconds, rampUpSeconds, etc.)
     * @throws InterruptedException if the execution thread is interrupted
     */
    void execute(ExecutionContext context, TestConfig config) throws InterruptedException;

    /**
     * Signals the engine to stop gracefully. Called from a different thread
     * than the one running {@link #execute}. The engine should exit its
     * main loop within one request cycle + think time.
     */
    void stop();

}
