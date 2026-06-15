package com.stormapi.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Generic API response envelope for consistent frontend parsing.
 * Every endpoint returns ApiResponse<T> — either success with data, or error with details.
 *
 * Success: { "success": true, "data": {...}, "timestamp": "...", "path": "..." }
 * Error:   { "success": false, "error": {...}, "timestamp": "...", "path": "..." }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ApiError error;
    private final Instant timestamp;
    private final String path;

    /**
     * Create a success response with data payload.
     */
    public static <T> ApiResponse<T> success(T data, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * Create a success response without data (e.g., 204 No Content).
     */
    public static <T> ApiResponse<T> success(String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * Create an error response.
     */
    public static <T> ApiResponse<T> error(int status, String statusReason,
                                            String message, String errorCode, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ApiError.builder()
                        .status(status)
                        .error(statusReason)
                        .message(message)
                        .errorCode(errorCode)
                        .build())
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * Create an error response with field-level validation errors.
     */
    public static <T> ApiResponse<T> validationError(String message,
                                                      Map<String, String> fieldErrors, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ApiError.builder()
                        .status(400)
                        .error("Bad Request")
                        .message(message)
                        .errorCode("VALIDATION_FAILED")
                        .fieldErrors(fieldErrors)
                        .build())
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * Inner class representing error details.
     */
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiError {
        private final int status;
        private final String error;
        private final String message;
        private final String errorCode;
        private final Map<String, String> fieldErrors;
    }

}
