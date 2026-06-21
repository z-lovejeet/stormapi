package com.stormapi.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation is invalid for the current state.
 * Examples: deleting a running test, stopping a non-running test.
 * Maps to HTTP 409 CONFLICT.
 */
public class InvalidStateTransitionException extends ApiException {

    public InvalidStateTransitionException(String message) {
        super(message, HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION");
    }

}
