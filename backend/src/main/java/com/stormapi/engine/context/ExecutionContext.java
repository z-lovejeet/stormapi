package com.stormapi.engine.context;

import com.stormapi.engine.http.RequestResult;
import com.stormapi.engine.http.RequestSpec;
import com.stormapi.engine.user.ThinkTimeStrategy;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Shared, thread-safe state container for a single test run.
 *
 * Provides the immutable RequestSpec, running flag, result callback,
 * and think-time strategy to all virtual users in a test.
 *
 * Thread-safety guarantees:
 * - AtomicBoolean for running flag (lock-free reads from 10K+ virtual threads)
 * - AtomicInteger for active user count (lock-free increment/decrement)
 * - requestSpec and thinkTimeStrategy are immutable — safe for concurrent reads
 * - resultConsumer is set once at construction — effectively final
 */
public class ExecutionContext {

    private final RequestSpec requestSpec;
    private final ThinkTimeStrategy thinkTimeStrategy;
    private final Consumer<RequestResult> resultConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private volatile int maxRetries;
    private volatile Instant startedAt;

    /**
     * Creates an execution context for a test run.
     *
     * @param requestSpec     immutable request template shared by all virtual users
     * @param thinkTimeStrategy  pause strategy between requests
     * @param resultConsumer  callback for each request result (must be thread-safe)
     */
    public ExecutionContext(RequestSpec requestSpec,
                           ThinkTimeStrategy thinkTimeStrategy,
                           Consumer<RequestResult> resultConsumer) {
        this.requestSpec = requestSpec;
        this.thinkTimeStrategy = thinkTimeStrategy;
        this.resultConsumer = resultConsumer;
        this.maxRetries = 0;
    }

    public RequestSpec getRequestSpec() {
        return requestSpec;
    }

    public ThinkTimeStrategy getThinkTimeStrategy() {
        return thinkTimeStrategy;
    }

    /**
     * Check if the test is still running.
     * Called at the top of each virtual user loop iteration.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Starts the test — all virtual users can begin their loops.
     */
    public void start() {
        startedAt = Instant.now();
        running.set(true);
    }

    /**
     * Stops the test gracefully — virtual users exit at their next loop check.
     * No Thread.interrupt() is used during normal shutdown.
     */
    public void stop() {
        running.set(false);
    }

    /**
     * Records a request result by forwarding it to the configured consumer.
     * The consumer is expected to be thread-safe (e.g., MetricsCollector in Phase 5).
     */
    public void recordResult(RequestResult result) {
        if (resultConsumer != null) {
            resultConsumer.accept(result);
        }
    }

    /**
     * Returns the number of currently active virtual users.
     */
    public int getActiveUsers() {
        return activeUsers.get();
    }

    /**
     * Increment active user count — called when a virtual user starts its loop.
     */
    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
    }

    /**
     * Decrement active user count — called in the finally block when a virtual user exits.
     */
    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Returns the instant when the test was started.
     */
    public Instant getStartedAt() {
        return startedAt;
    }

}
