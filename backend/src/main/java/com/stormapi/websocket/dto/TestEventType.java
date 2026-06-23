package com.stormapi.websocket.dto;

/**
 * Test lifecycle event types broadcast via WebSocket.
 */
public enum TestEventType {
    TEST_CREATED,
    TEST_STARTED,
    TEST_RUNNING,
    TEST_PROGRESS,
    TEST_COMPLETED,
    TEST_FAILED,
    TEST_CANCELLED,
    TEST_STOPPED
}
