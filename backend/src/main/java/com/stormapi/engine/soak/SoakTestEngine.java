package com.stormapi.engine.soak;

import com.stormapi.engine.AbstractTestEngine;
import com.stormapi.engine.analysis.EngineAnalysisResult;
import com.stormapi.engine.analysis.TrendAnalyzer;
import com.stormapi.engine.analysis.TrendAnalyzer.DataPoint;
import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpClientFactory;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.metrics.LiveMetricsSnapshot;
import com.stormapi.engine.ramp.RampUpStrategy;
import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.TestType;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Soak Test Engine — sustained load over an extended period with drift detection.
 *
 * Algorithm:
 * 1. Ramp up all users (using configured ramp-up strategy)
 * 2. Sustain load for the remaining duration
 * 3. Sample avgResponseTimeMs every SAMPLE_INTERVAL_SECONDS
 * 4. After test completes, run TrendAnalyzer on collected samples
 * 5. Report degradation slope and detection flag
 *
 * Designed for long-running tests (10-30+ minutes).
 * Detects memory leaks and performance drift that only appear under sustained load.
 */
public class SoakTestEngine extends AbstractTestEngine {

    private static final int SAMPLE_INTERVAL_SECONDS = 10;
    private static final double DEGRADATION_THRESHOLD_MS = 0.1; // 0.1 ms/sec = 6 ms/min drift

    private HttpClient httpClient;

    @Override
    public TestType getSupportedType() {
        return TestType.SOAK;
    }

    @Override
    protected void doExecute(ExecutionContext context, TestConfig config) throws InterruptedException {
        httpClient = HttpClientFactory.create(Duration.ofMillis(config.getTimeoutMs()));
        HttpRequestExecutor executor = new HttpRequestExecutor(httpClient);

        // 1. Ramp up all users
        RampUpStrategy rampUp = RampUpStrategy.fromConfig(config);
        rampUp.execute(config.getVirtualUsers(), context, executor, userThreads::add);

        // 2. Sustain and collect samples
        int sustainSeconds = Math.max(0, config.getDurationSeconds() - config.getRampUpSeconds());
        List<DataPoint> samples = new ArrayList<>();

        int elapsed = 0;
        while (!stopped && elapsed < sustainSeconds) {
            int sleepTime = Math.min(SAMPLE_INTERVAL_SECONDS, sustainSeconds - elapsed);
            sleepInterruptibly(sleepTime * 1000L);
            elapsed += sleepTime;

            // Collect sample
            if (!stopped && context.getSnapshotSupplier() != null) {
                LiveMetricsSnapshot snapshot = context.getSnapshotSupplier().get();
                if (snapshot.totalRequests() > 0) {
                    samples.add(new DataPoint(elapsed, snapshot.avgResponseTimeMs()));
                }
            }
        }

        // 3. Signal stop and wait
        context.stop();
        awaitAllUsers(Duration.ofSeconds(10));

        // 4. Run trend analysis on collected samples
        TrendAnalyzer.TrendResult trendResult =
                TrendAnalyzer.analyze(samples, DEGRADATION_THRESHOLD_MS);
        context.setAnalysisResult(
                EngineAnalysisResult.soak(trendResult.slope(), trendResult.degradationDetected()));
    }

    @Override
    protected void onAfterExecute(ExecutionContext context) {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

}
