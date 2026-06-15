package com.stormapi.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when test configuration has logical errors beyond basic validation.
 * For example: rampUpSeconds > durationSeconds, spikeUsers not set for SPIKE test.
 * Maps to HTTP 422 UNPROCESSABLE_ENTITY.
 */
public class InvalidTestConfigException extends ApiException {

    public InvalidTestConfigException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TEST_CONFIG");
    }

}
