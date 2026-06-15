package com.stormapi.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource (TestConfig, TestResult, Collection, etc.) is not found.
 * Maps to HTTP 404 NOT_FOUND.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(
                resourceName + " with id " + id + " not found",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

}
