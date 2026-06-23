package com.stormapi.websocket.session;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks active WebSocket sessions and their subscriptions.
 *
 * Thread-safe: all data structures are concurrent collections.
 * Used for monitoring (health indicator) and diagnostics.
 */
@Component
public class WebSocketSessionTracker {

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * Registers a new WebSocket session.
     */
    public void registerSession(String sessionId, String remoteAddress) {
        sessions.put(sessionId, new SessionInfo(sessionId, remoteAddress,
                Instant.now(), new CopyOnWriteArraySet<>()));
    }

    /**
     * Removes a disconnected session and all its subscriptions.
     */
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Records a subscription for a session.
     */
    public void addSubscription(String sessionId, String destination) {
        SessionInfo info = sessions.get(sessionId);
        if (info != null) {
            info.subscriptions().add(destination);
        }
    }

    /**
     * Removes a subscription from a session.
     */
    public void removeSubscription(String sessionId, String destination) {
        SessionInfo info = sessions.get(sessionId);
        if (info != null) {
            info.subscriptions().remove(destination);
        }
    }

    /**
     * Returns the total number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Returns the number of subscribers to a specific destination.
     */
    public long getSubscriberCount(String destination) {
        return sessions.values().stream()
                .filter(info -> info.subscriptions().contains(destination))
                .count();
    }

    /**
     * Returns an unmodifiable snapshot of all active sessions.
     */
    public Map<String, SessionInfo> getAllSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    /**
     * Returns the total count of all subscriptions across all sessions.
     */
    public int getTotalSubscriptionCount() {
        return sessions.values().stream()
                .mapToInt(info -> info.subscriptions().size())
                .sum();
    }

    /**
     * Immutable session metadata record.
     */
    public record SessionInfo(
            String sessionId,
            String remoteAddress,
            Instant connectedAt,
            Set<String> subscriptions
    ) {}

}
