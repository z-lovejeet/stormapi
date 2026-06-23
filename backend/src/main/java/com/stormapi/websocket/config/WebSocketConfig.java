package com.stormapi.websocket.config;

import com.stormapi.websocket.handler.WebSocketErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * Configures STOMP over SockJS WebSocket infrastructure.
 *
 * Broker topology:
 * - Simple in-memory broker on "/topic" prefix
 * - Application destination prefix "/app" (for client-to-server messages — unused in Phase 9)
 * - SockJS endpoint at "/ws" with fallback transports
 *
 * Heartbeat: 10-second server/client interval to detect stale connections.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketErrorHandler errorHandler;

    public WebSocketConfig(WebSocketErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000});
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Set the error handler for STOMP protocol errors
        registry.setErrorHandler(errorHandler);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024);  // 64KB max message size
        registration.setSendBufferSizeLimit(512 * 1024);  // 512KB send buffer
        registration.setSendTimeLimit(20 * 1000);  // 20s send timeout
    }

}
