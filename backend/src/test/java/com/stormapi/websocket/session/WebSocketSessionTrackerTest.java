package com.stormapi.websocket.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WebSocketSessionTracker Unit Tests")
class WebSocketSessionTrackerTest {

    private WebSocketSessionTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new WebSocketSessionTracker();
    }

    @Test
    @DisplayName("registerSession — increments active count")
    void registerSession_incrementsCount() {
        tracker.registerSession("session-1", "127.0.0.1");
        tracker.registerSession("session-2", "127.0.0.2");

        assertEquals(2, tracker.getActiveSessionCount());
    }

    @Test
    @DisplayName("removeSession — decrements active count")
    void removeSession_decrementsCount() {
        tracker.registerSession("session-1", "127.0.0.1");
        tracker.registerSession("session-2", "127.0.0.2");
        tracker.removeSession("session-1");

        assertEquals(1, tracker.getActiveSessionCount());
    }

    @Test
    @DisplayName("removeSession — nonexistent session is no-op")
    void removeSession_nonexistent_noOp() {
        tracker.registerSession("session-1", "127.0.0.1");
        tracker.removeSession("nonexistent");

        assertEquals(1, tracker.getActiveSessionCount());
    }

    @Test
    @DisplayName("addSubscription — tracked correctly")
    void addSubscription_tracked() {
        tracker.registerSession("s1", "127.0.0.1");
        tracker.addSubscription("s1", "/topic/metrics/42");
        tracker.addSubscription("s1", "/topic/events/42");

        assertEquals(2, tracker.getTotalSubscriptionCount());
    }

    @Test
    @DisplayName("getSubscriberCount — returns correct count per destination")
    void getSubscriberCount_correct() {
        tracker.registerSession("s1", "127.0.0.1");
        tracker.registerSession("s2", "127.0.0.2");
        tracker.addSubscription("s1", "/topic/metrics/42");
        tracker.addSubscription("s2", "/topic/metrics/42");
        tracker.addSubscription("s2", "/topic/metrics/99");

        assertEquals(2, tracker.getSubscriberCount("/topic/metrics/42"));
        assertEquals(1, tracker.getSubscriberCount("/topic/metrics/99"));
        assertEquals(0, tracker.getSubscriberCount("/topic/metrics/0"));
    }

    @Test
    @DisplayName("removeSubscription — decrements count")
    void removeSubscription_decrementsCount() {
        tracker.registerSession("s1", "127.0.0.1");
        tracker.addSubscription("s1", "/topic/metrics/42");
        tracker.removeSubscription("s1", "/topic/metrics/42");

        assertEquals(0, tracker.getTotalSubscriptionCount());
    }

    @Test
    @DisplayName("removeSession — also removes all subscriptions")
    void removeSession_removesSubscriptions() {
        tracker.registerSession("s1", "127.0.0.1");
        tracker.addSubscription("s1", "/topic/metrics/42");
        tracker.addSubscription("s1", "/topic/events/42");
        tracker.removeSession("s1");

        assertEquals(0, tracker.getTotalSubscriptionCount());
        assertEquals(0, tracker.getSubscriberCount("/topic/metrics/42"));
    }

    @Test
    @DisplayName("getAllSessions — returns unmodifiable snapshot")
    void getAllSessions_unmodifiable() {
        tracker.registerSession("s1", "127.0.0.1");

        var sessions = tracker.getAllSessions();
        assertThrows(UnsupportedOperationException.class, () -> sessions.put("s2", null));
    }

}
