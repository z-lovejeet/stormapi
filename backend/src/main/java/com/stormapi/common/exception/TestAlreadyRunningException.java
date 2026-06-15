package com.stormapi.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to start a test that is already running.
 * Maps to HTTP 409 CONFLICT.
 */
public class TestAlreadyRunningException extends ApiException {

    public TestAlreadyRunningException(Long testId) {
        super(
                "Test " + testId + " is already running",
                HttpStatus.CONFLICT,
                "TEST_ALREADY_RUNNING"
        );
    }

}
