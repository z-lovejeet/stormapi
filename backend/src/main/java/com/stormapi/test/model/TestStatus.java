package com.stormapi.test.model;

/**
 * Tracks the lifecycle state of a test execution.
 *
 * State transitions:
 *   CREATED → QUEUED → RUNNING → COMPLETED
 *                               → FAILED
 *                     → CANCELLED
 */
public enum TestStatus {
    CREATED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
