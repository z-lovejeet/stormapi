package com.stormapi.engine.user;

import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.http.RequestResult;
import com.stormapi.engine.http.RequestSpec;

/**
 * Simulates a single virtual user performing a request loop.
 *
 * Each instance runs on its own virtual thread — Java 21 can handle 100K+
 * virtual threads. This is the workhorse: 10,000 instances of this class
 * run concurrently during a load test.
 *
 * Loop: send request → record result → think time → repeat
 * Exit: when ExecutionContext.isRunning() returns false
 *
 * Thread-safety: each instance is used by exactly one virtual thread.
 * All shared state lives in ExecutionContext (which uses atomics).
 */
public class VirtualUserSimulator implements Runnable {

    private final ExecutionContext context;
    private final HttpRequestExecutor executor;

    public VirtualUserSimulator(ExecutionContext context, HttpRequestExecutor executor) {
        this.context = context;
        this.executor = executor;
    }

    @Override
    public void run() {
        context.incrementActiveUsers();
        try {
            while (context.isRunning()) {
                RequestSpec spec = context.getRequestSpec();

                // Execute with retry logic
                RequestResult result = executeWithRetry(spec, context.getMaxRetries());

                // Report result to the metrics consumer
                context.recordResult(result);

                // Apply think-time between requests (if still running)
                if (context.isRunning()) {
                    context.getThinkTimeStrategy().apply();
                }
            }
        } catch (InterruptedException e) {
            // Restore interrupt flag — clean exit for forced shutdown
            Thread.currentThread().interrupt();
        } finally {
            context.decrementActiveUsers();
        }
    }

    /**
     * Executes a request with optional retries.
     * Returns the final result — either a success or the last failure.
     */
    private RequestResult executeWithRetry(RequestSpec spec, int maxRetries) {
        RequestResult result = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            result = executor.execute(spec);

            // If successful or last attempt, return immediately
            if (result.success() || attempt == maxRetries) {
                return result;
            }

            // Brief back-off between retries to avoid hammering a struggling server
            if (context.isRunning()) {
                try {
                    Thread.sleep(50L * (attempt + 1)); // 50ms, 100ms, 150ms...
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return result;
                }
            }
        }
        return result;
    }

}
