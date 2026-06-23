package com.stormapi.websocket.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WebSocketErrorHandler Unit Tests")
class WebSocketErrorHandlerTest {

    private WebSocketErrorHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebSocketErrorHandler();
    }

    @Test
    @DisplayName("handleClientMessageProcessingError — returns structured error")
    void handleError_returnsStructuredError() {
        Message<byte[]> clientMsg = MessageBuilder.withPayload("test".getBytes()).build();

        Message<byte[]> result = handler.handleClientMessageProcessingError(
                clientMsg, new RuntimeException("Something broke"));

        assertNotNull(result);
        String payload = new String(result.getPayload(), StandardCharsets.UTF_8);
        assertTrue(payload.contains("Message processing failed"));
        assertTrue(payload.contains("Something broke"));
        // Should not contain stack trace
        assertFalse(payload.contains("at org."));
    }

    @Test
    @DisplayName("handleClientMessageProcessingError — null message handled")
    void handleError_nullMessage() {
        Message<byte[]> clientMsg = MessageBuilder.withPayload("test".getBytes()).build();

        Message<byte[]> result = handler.handleClientMessageProcessingError(
                clientMsg, new RuntimeException());

        assertNotNull(result);
        String payload = new String(result.getPayload(), StandardCharsets.UTF_8);
        assertTrue(payload.contains("Unknown error"));
    }

    @Test
    @DisplayName("handleClientMessageProcessingError — strips stack traces from message")
    void handleError_stripsStackTrace() {
        Message<byte[]> clientMsg = MessageBuilder.withPayload("test".getBytes()).build();
        String messageWithStack = "Error occurred\nat com.example.Foo.bar(Foo.java:42)";

        Message<byte[]> result = handler.handleClientMessageProcessingError(
                clientMsg, new RuntimeException(messageWithStack));

        String payload = new String(result.getPayload(), StandardCharsets.UTF_8);
        assertFalse(payload.contains("Foo.java"));
    }

}
