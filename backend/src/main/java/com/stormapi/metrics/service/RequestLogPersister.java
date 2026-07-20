package com.stormapi.metrics.service;

import com.stormapi.engine.http.RequestResult;
import com.stormapi.metrics.model.RequestLog;
import com.stormapi.metrics.repository.RequestLogRepository;
import com.stormapi.test.model.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Collects individual request results during test execution and batch-persists
 * them to the database when the test completes.
 *
 * Uses a sampled approach: captures up to MAX_LOGS_PER_TEST entries to avoid
 * excessive memory usage on high-volume tests.
 *
 * Thread-safe: captureResult is called from virtual user threads concurrently.
 */
@Service
public class RequestLogPersister {

    private static final Logger log = LoggerFactory.getLogger(RequestLogPersister.class);

    /**
     * Maximum number of request logs to persist per test.
     * At 100 users × 60 seconds × ~10 rps = ~60,000 requests.
     * We sample up to 10,000 to keep DB size manageable.
     */
    static final int MAX_LOGS_PER_TEST = 10_000;

    /** Batch size for saveAll() calls to avoid huge single transactions. */
    private static final int BATCH_SIZE = 500;

    private final RequestLogRepository requestLogRepository;

    /** Per-test capture state: configId → CaptureState */
    private final ConcurrentHashMap<Long, CaptureState> activeCaptures = new ConcurrentHashMap<>();

    public RequestLogPersister(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    /**
     * Starts capturing request logs for a test.
     */
    public void startCapturing(Long configId) {
        activeCaptures.put(configId, new CaptureState());
        log.info("Request log persistence started for test {}", configId);
    }

    /**
     * Creates a Consumer that captures request results for a specific test.
     * Called from TestOrchestrator to compose with MetricsCollector and RequestLogBroadcaster.
     */
    public Consumer<RequestResult> createConsumer(Long configId, String url, String method) {
        return result -> captureResult(configId, result, url, method);
    }

    /**
     * Captures a single request result. Thread-safe, non-blocking.
     * Stops capturing once MAX_LOGS_PER_TEST is reached (sampling by truncation).
     */
    private void captureResult(Long configId, RequestResult result, String url, String method) {
        CaptureState state = activeCaptures.get(configId);
        if (state == null || state.count >= MAX_LOGS_PER_TEST) {
            return;
        }

        state.queue.offer(new CapturedLog(result, url, method));
        state.count++;
    }

    /**
     * Stops capturing and batch-persists all collected request logs to the database.
     *
     * @param configId   the test config ID
     * @param testResult the TestResult entity to associate logs with
     */
    public void stopAndPersist(Long configId, TestResult testResult) {
        CaptureState state = activeCaptures.remove(configId);
        if (state == null || testResult == null) {
            return;
        }

        List<RequestLog> batch = new ArrayList<>(BATCH_SIZE);
        int totalPersisted = 0;

        CapturedLog captured;
        while ((captured = state.queue.poll()) != null) {
            RequestLog entity = RequestLog.builder()
                    .testResult(testResult)
                    .timestamp(captured.result.timestamp())
                    .url(captured.url)
                    .method(captured.method)
                    .statusCode(captured.result.statusCode())
                    .responseTimeMs((long) captured.result.responseTimeMs())
                    .responseSize(captured.result.responseBodySize())
                    .errorMessage(captured.result.errorMessage())
                    .success(captured.result.success())
                    .build();

            batch.add(entity);

            if (batch.size() >= BATCH_SIZE) {
                try {
                    requestLogRepository.saveAll(batch);
                    totalPersisted += batch.size();
                } catch (Exception e) {
                    log.warn("Failed to persist request log batch: {}", e.getMessage());
                }
                batch.clear();
            }
        }

        // Flush remaining
        if (!batch.isEmpty()) {
            try {
                requestLogRepository.saveAll(batch);
                totalPersisted += batch.size();
            } catch (Exception e) {
                log.warn("Failed to persist final request log batch: {}", e.getMessage());
            }
        }

        log.info("Persisted {} request logs for test {}", totalPersisted, configId);
    }

    // ── Internal State ─────────────────────────────────────────────

    private static class CaptureState {
        final ConcurrentLinkedQueue<CapturedLog> queue = new ConcurrentLinkedQueue<>();
        volatile int count = 0;
    }

    private record CapturedLog(RequestResult result, String url, String method) {}
}
