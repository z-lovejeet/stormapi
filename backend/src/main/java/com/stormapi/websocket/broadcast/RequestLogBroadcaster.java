package com.stormapi.websocket.broadcast;

import com.stormapi.engine.http.RequestResult;
import com.stormapi.websocket.dto.RequestLogMessage;
import com.stormapi.websocket.dto.RequestLogMessage.RequestLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Captures, batches, and broadcasts request logs during test execution.
 *
 * Architecture:
 * - Hot path: {@link #captureResult} called per request from virtual user threads.
 *   Uses ConcurrentLinkedDeque.offerLast() — O(1), non-blocking.
 * - Cool path: {@link #flush} called ~1/s from orchestrator's snapshot timer.
 *   Drains up to BATCH_SIZE entries and broadcasts.
 *
 * Buffer is bounded: oldest entries evicted when MAX_BUFFER_SIZE exceeded.
 * This is a sampled stream — complete logs available via REST API post-test.
 */
@Service
public class RequestLogBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(RequestLogBroadcaster.class);

    static final int MAX_BUFFER_SIZE = 100;
    static final int BATCH_SIZE = 50;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Per-test buffer: testId → deque of captured results.
     */
    private final ConcurrentHashMap<Long, BufferState> activeBuffers = new ConcurrentHashMap<>();

    public RequestLogBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Initializes the capture buffer for a test.
     */
    public void startCapturing(Long testId) {
        activeBuffers.put(testId, new BufferState());
        log.info("Request log capturing started for test {}", testId);
    }

    /**
     * Captures a single request result into the buffer.
     * Called from the hot path — MUST be non-blocking.
     *
     * @param testId the test config ID
     * @param result the request result to capture
     * @param url    the requested URL (from RequestSpec)
     * @param method the HTTP method (from RequestSpec)
     */
    public void captureResult(Long testId, RequestResult result, String url, String method) {
        BufferState state = activeBuffers.get(testId);
        if (state == null) return;

        // Add to deque (non-blocking)
        state.deque.offerLast(new CapturedEntry(result, url, method));
        int size = state.size.incrementAndGet();

        // Evict oldest if over limit
        while (size > MAX_BUFFER_SIZE) {
            if (state.deque.pollFirst() != null) {
                size = state.size.decrementAndGet();
            } else {
                break;
            }
        }
    }

    /**
     * Creates a Consumer that captures results for a specific test.
     * Used by TestOrchestrator to compose with MetricsCollector consumer.
     *
     * @param testId the test config ID
     * @param url    the target URL from RequestSpec
     * @param method the HTTP method from RequestSpec
     * @return consumer that feeds captureResult
     */
    public Consumer<RequestResult> createConsumer(Long testId, String url, String method) {
        return result -> captureResult(testId, result, url, method);
    }

    /**
     * Drains the buffer and broadcasts a batch of request logs.
     * Called ~1/s from the orchestrator's snapshot timer.
     */
    public void flush(Long testId) {
        BufferState state = activeBuffers.get(testId);
        if (state == null) return;

        List<RequestLogEntry> entries = new ArrayList<>(BATCH_SIZE);
        int drained = 0;

        while (drained < BATCH_SIZE) {
            CapturedEntry captured = state.deque.pollFirst();
            if (captured == null) break;

            state.size.decrementAndGet();
            entries.add(RequestLogEntry.from(captured.result, captured.url, captured.method));
            drained++;
        }

        if (!entries.isEmpty()) {
            try {
                RequestLogMessage message = new RequestLogMessage(testId, entries);
                messagingTemplate.convertAndSend("/topic/logs/" + testId, message);
                log.debug("Broadcast {} request logs for test {}", entries.size(), testId);
            } catch (Exception ex) {
                log.warn("Failed to broadcast logs for test {}: {}", testId, ex.getMessage());
            }
        }
    }

    /**
     * Stops capturing for a test. Drains any remaining entries and broadcasts.
     */
    public void stopCapturing(Long testId) {
        BufferState state = activeBuffers.remove(testId);
        if (state != null) {
            // Final flush of remaining entries
            List<RequestLogEntry> remaining = new ArrayList<>();
            CapturedEntry captured;
            while ((captured = state.deque.pollFirst()) != null) {
                remaining.add(RequestLogEntry.from(captured.result, captured.url, captured.method));
            }
            if (!remaining.isEmpty()) {
                try {
                    messagingTemplate.convertAndSend("/topic/logs/" + testId,
                            new RequestLogMessage(testId, remaining));
                } catch (Exception ex) {
                    log.warn("Failed to send final logs for test {}: {}", testId, ex.getMessage());
                }
            }
            log.info("Request log capturing stopped for test {}", testId);
        }
    }

    /**
     * Returns whether capturing is active for a test.
     */
    public boolean isActive(Long testId) {
        return activeBuffers.containsKey(testId);
    }

    /**
     * Internal buffer state for one test.
     */
    private static class BufferState {
        final ConcurrentLinkedDeque<CapturedEntry> deque = new ConcurrentLinkedDeque<>();
        final AtomicInteger size = new AtomicInteger(0);
    }

    /**
     * Wraps a RequestResult with its URL and method context.
     */
    private record CapturedEntry(RequestResult result, String url, String method) {}

}
