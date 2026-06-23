package com.stormapi.websocket.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for Spring WebSocket session lifecycle events.
 * Forwards connect/disconnect events to {@link WebSocketSessionTracker}.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final WebSocketSessionTracker sessionTracker;

    public WebSocketEventListener(WebSocketSessionTracker sessionTracker) {
        this.sessionTracker = sessionTracker;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        sessionTracker.registerSession(sessionId, "unknown");
        log.info("WebSocket session connected: {}", sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        sessionTracker.removeSession(sessionId);
        log.info("WebSocket session disconnected: {}, close status: {}",
                sessionId, event.getCloseStatus());
    }

}
