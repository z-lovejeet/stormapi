package com.stormapi.engine;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.user.VirtualUserSimulator;
import com.stormapi.test.model.TestConfig;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Template method base for all test engines.
 *
 * Provides shared lifecycle management:
 * - User spawning via virtual threads
 * - User tracking in a thread-safe list
 * - Graceful shutdown with timeout-based join
 * - Before/after hooks for engine-specific setup/cleanup
 *
 * Subclasses implement {@link #doExecute} with their unique load pattern.
 * The template ensures cleanup runs even on failure.
 *
 * Thread-safety:
 * - {@code stopped} is volatile — single writer ({@link #stop}), multiple readers
 * - {@code userThreads} is CopyOnWriteArrayList — safe for concurrent adds during
 *   ramp-up and reads during shutdown
 */
public abstract class AbstractTestEngine implements TestEngine {

    protected final List<Thread> userThreads = new CopyOnWriteArrayList<>();
    protected volatile boolean stopped = false;

    /**
     * Template method — runs the full engine lifecycle.
     * Guarantees cleanup via try/finally.
     */
    @Override
    public final void execute(ExecutionContext context, TestConfig config) throws InterruptedException {
        try {
            onBeforeExecute(context);
            doExecute(context, config);
        } finally {
            onAfterExecute(context);
        }
    }

    /**
     * The engine's core logic. Subclasses implement their unique load pattern here.
     * Called between {@link #onBeforeExecute} and {@link #onAfterExecute}.
     */
    protected abstract void doExecute(ExecutionContext context, TestConfig config) throws InterruptedException;

    /**
     * Hook called before execution begins. Override for engine-specific setup.
     * Default: no-op.
     */
    protected void onBeforeExecute(ExecutionContext context) {
        // subclass hook
    }

    /**
     * Hook called after execution completes (even on failure).
     * Default: no-op.
     */
    protected void onAfterExecute(ExecutionContext context) {
        // subclass hook
    }

    /**
     * Signals the engine to stop gracefully.
     * Called from a different thread than the one running execute().
     */
    @Override
    public void stop() {
        stopped = true;
    }

    /**
     * Spawns a virtual user thread running {@link VirtualUserSimulator}.
     * Adds the thread to the tracking list for lifecycle management.
     *
     * @return the started virtual thread
     */
    protected Thread spawnUser(int userId, ExecutionContext context, HttpRequestExecutor executor) {
        Thread thread = Thread.ofVirtual()
                .name("storm-user-", userId)
                .start(new VirtualUserSimulator(context, executor));
        userThreads.add(thread);
        return thread;
    }

    /**
     * Waits for all spawned user threads to complete within the given timeout.
     * If threads are still alive after the timeout, they are interrupted.
     *
     * @param timeout maximum time to wait for user threads to finish
     */
    protected void awaitAllUsers(Duration timeout) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        for (Thread thread : userThreads) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) break;

            thread.join(Duration.ofNanos(remainingNanos));
        }

        // Force-interrupt any threads still alive after timeout
        for (Thread thread : userThreads) {
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }
    }

    /**
     * Sleeps for the specified duration in 1-second chunks.
     * Checks the stopped flag between each chunk for prompt cancellation response.
     *
     * @param seconds total seconds to sleep
     */
    protected void sleepInterruptibly(int seconds) throws InterruptedException {
        sleepInterruptibly(seconds * 1000L);
    }

    /**
     * Sleeps for the specified duration in chunks, checking the stopped flag
     * between each chunk for prompt cancellation response.
     *
     * @param millis total milliseconds to sleep
     */
    protected void sleepInterruptibly(long millis) throws InterruptedException {
        long remaining = millis;
        while (remaining > 0 && !stopped) {
            long chunk = Math.min(remaining, 1000L);
            Thread.sleep(chunk);
            remaining -= chunk;
        }
    }

    /**
     * Removes the last {@code count} user threads by interrupting them.
     * Used by SpikeTestEngine (drop spike users) and BreakpointTestEngine
     * (reduce users during binary search refinement).
     *
     * Interrupted threads exit via the InterruptedException catch in
     * VirtualUserSimulator.run() — the finally block decrements activeUsers.
     *
     * @param count number of users to remove (from the tail of the list)
     */
    protected void removeUsers(int count) {
        int size = userThreads.size();
        int toRemove = Math.min(count, size);

        // Interrupt from the tail — LIFO order
        for (int i = size - 1; i >= size - toRemove; i--) {
            Thread thread = userThreads.get(i);
            thread.interrupt();
        }

        // Remove interrupted threads from tracking list
        // CopyOnWriteArrayList: removal creates a new copy — safe during iteration
        for (int i = size - 1; i >= size - toRemove; i--) {
            userThreads.remove(i);
        }
    }

}
