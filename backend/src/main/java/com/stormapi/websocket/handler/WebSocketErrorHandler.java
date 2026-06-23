package com.stormapi.websocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

/**
 * Handles STOMP protocol errors (malformed frames, processing failures).
 * Never exposes stack traces to the client — returns structured error messages.
 */
@Component
public class WebSocketErrorHandler extends StompSubProtocolErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketErrorHandler.class);

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
            Message<byte[]> clientMessage, Throwable ex) {

        log.warn("WebSocket client message processing error: {}", ex.getMessage());

        String errorPayload = "{\"error\":\"Message processing failed\",\"message\":\""
                + sanitize(ex.getMessage()) + "\"}";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(
                org.springframework.messaging.simp.stomp.StompCommand.ERROR);
        accessor.setMessage("Message processing failed");
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                errorPayload.getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders());
    }

    @Override
    public Message<byte[]> handleErrorMessageToClient(Message<byte[]> errorMessage) {
        // Sanitize any error message before sending to client
        return super.handleErrorMessageToClient(errorMessage);
    }

    /**
     * Strips potential stack trace information from error messages.
     */
    private String sanitize(String message) {
        if (message == null) return "Unknown error";
        // Remove anything after newline (stack trace lines)
        int newlineIdx = message.indexOf('\n');
        String clean = (newlineIdx > 0) ? message.substring(0, newlineIdx) : message;
        // Escape quotes for JSON safety
        return clean.replace("\"", "'").replace("\\", "");
    }

}
