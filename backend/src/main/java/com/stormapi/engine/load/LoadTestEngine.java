package com.stormapi.engine.load;

import com.stormapi.engine.AbstractTestEngine;
import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpClientFactory;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.ramp.RampUpStrategy;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Load Test Engine — the first concrete test engine.
 *
 * Executes a standard load test:
 * 1. Create HTTP client and executor
 * 2. Ramp up virtual users according to the configured strategy
 * 3. Sustain load for the remaining duration
 * 4. Signal stop and wait for all users to complete
 * 5. Close the HTTP client
 *
 * The engine manages its own HttpClient lifecycle to prevent
 * connection pool contamination between test runs.
 */
public class LoadTestEngine extends AbstractTestEngine {

    private HttpClient httpClient;

    @Override
    public TestType getSupportedType() {
        return TestType.LOAD;
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected void doExecute(ExecutionContext context, TestConfig config) throws InterruptedException {
        // 1. Create HTTP client with configured timeout
        httpClient = HttpClientFactory.create(Duration.ofMillis(config.getTimeoutMs()));
        HttpRequestExecutor executor = new HttpRequestExecutor(httpClient);

        // 2. Determine ramp-up strategy
        RampUpStrategy rampUp = RampUpStrategy.fromConfig(config);

        // 3. Execute ramp-up — spawns virtual users over time
        rampUp.execute(config.getVirtualUsers(), context, executor, userThreads::add);

        // 4. Calculate sustain duration (total - rampUp)
        int sustainSeconds = Math.max(0, config.getDurationSeconds() - config.getRampUpSeconds());

        // 5. Sustain load — sleep in 1s chunks to detect stop requests
        sleepInterruptibly(sustainSeconds);

        // 6. Signal all users to stop
        context.stop();

        // 7. Wait for all users to finish (10s grace period)
        awaitAllUsers(Duration.ofSeconds(10));
    }

    @Override
    protected void onAfterExecute(ExecutionContext context) {
        // Close HttpClient to release connection pool
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

}
