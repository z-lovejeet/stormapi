package com.stormapi.websocket.broadcast;

import com.stormapi.engine.metrics.LiveMetricsSnapshot;
import com.stormapi.websocket.dto.LiveMetricsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Broadcasts live metrics snapshots to WebSocket subscribers.
 *
 * The TestOrchestrator calls {@link #broadcast} from the snapshot timer
 * with the already-captured snapshot. No separate scheduled task needed.
 *
 * Tracks active broadcasts per test to prevent duplicates and
 * to support the health indicator.
 *
 * Thread-safe: ConcurrentHashMap for tracking, SimpMessagingTemplate is thread-safe.
 */
@Service
public class LiveMetricsBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LiveMetricsBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<Long, Boolean> activeBroadcasts = new ConcurrentHashMap<>();

    public LiveMetricsBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Marks a test as actively broadcasting.
     */
    public void startBroadcasting(Long testId) {
        activeBroadcasts.put(testId, Boolean.TRUE);
        log.info("Metrics broadcasting started for test {}", testId);
    }

    /**
     * Broadcasts a metrics snapshot for a test.
     * Called from the TestOrchestrator's snapshot timer (~1/s).
     *
     * @param testId   the test config ID
     * @param snapshot the metrics snapshot to broadcast
     */
    public void broadcast(Long testId, LiveMetricsSnapshot snapshot) {
        if (!activeBroadcasts.containsKey(testId)) {
            return; // Not actively broadcasting
        }

        try {
            LiveMetricsMessage message = LiveMetricsMessage.from(testId, snapshot);
            messagingTemplate.convertAndSend("/topic/metrics/" + testId, message);
        } catch (Exception ex) {
            log.warn("Failed to broadcast metrics for test {}: {}", testId, ex.getMessage());
        }
    }

    /**
     * Stops broadcasting for a test and sends one final snapshot.
     *
     * @param testId        the test config ID
     * @param finalSnapshot the final snapshot to broadcast (nullable)
     */
    public void stopBroadcasting(Long testId, LiveMetricsSnapshot finalSnapshot) {
        if (activeBroadcasts.remove(testId) != null) {
            // Send final snapshot
            if (finalSnapshot != null) {
                try {
                    LiveMetricsMessage message = LiveMetricsMessage.from(testId, finalSnapshot);
                    messagingTemplate.convertAndSend("/topic/metrics/" + testId, message);
                } catch (Exception ex) {
                    log.warn("Failed to send final metrics for test {}: {}", testId, ex.getMessage());
                }
            }
            log.info("Metrics broadcasting stopped for test {}", testId);
        }
    }

    /**
     * Returns whether broadcasting is active for a test.
     */
    public boolean isActive(Long testId) {
        return activeBroadcasts.containsKey(testId);
    }

    /**
     * Returns the number of tests currently broadcasting.
     */
    public int getActiveBroadcastCount() {
        return activeBroadcasts.size();
    }

}
