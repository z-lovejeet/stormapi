package com.stormapi.websocket.health;

import com.stormapi.websocket.broadcast.LiveMetricsBroadcaster;
import com.stormapi.websocket.session.WebSocketSessionTracker;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports WebSocket infrastructure health via /actuator/health.
 *
 * Reports:
 * - activeSessions: connected WebSocket clients
 * - totalSubscriptions: total topic subscriptions across all sessions
 * - activeBroadcasts: tests currently broadcasting metrics
 */
@Component
public class WebSocketHealthIndicator implements HealthIndicator {

    private final WebSocketSessionTracker sessionTracker;
    private final LiveMetricsBroadcaster metricsBroadcaster;

    public WebSocketHealthIndicator(WebSocketSessionTracker sessionTracker,
                                     LiveMetricsBroadcaster metricsBroadcaster) {
        this.sessionTracker = sessionTracker;
        this.metricsBroadcaster = metricsBroadcaster;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("activeSessions", sessionTracker.getActiveSessionCount())
                .withDetail("totalSubscriptions", sessionTracker.getTotalSubscriptionCount())
                .withDetail("activeBroadcasts", metricsBroadcaster.getActiveBroadcastCount())
                .build();
    }

}
