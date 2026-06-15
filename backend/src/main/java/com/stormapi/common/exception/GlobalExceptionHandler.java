package com.stormapi.common.exception;

import com.stormapi.common.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler that ensures every error returns structured JSON.
 * Never exposes stack traces to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all custom ApiException subclasses (404, 409, 422, etc.).
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException ex, HttpServletRequest request) {

        log.warn("API exception: {} — {}", ex.getErrorCode(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * Handles Jakarta Validation errors (@Valid annotation failures).
     * Returns field-level error messages for frontend form display.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);

        ApiResponse<Void> response = ApiResponse.validationError(
                "Validation failed",
                fieldErrors,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles type mismatch errors (e.g., String where Long expected in path variable).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Parameter '%s' must be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ApiResponse<Void> response = ApiResponse.error(
                400, "Bad Request", message, "TYPE_MISMATCH", request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles missing or malformed request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ApiResponse<Void> response = ApiResponse.error(
                400, "Bad Request",
                "Request body is missing or malformed",
                "INVALID_REQUEST_BODY",
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles HTTP method not supported (e.g., PUT on a POST-only endpoint).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String message = String.format("Method '%s' is not supported for this endpoint", ex.getMethod());

        ApiResponse<Void> response = ApiResponse.error(
                405, "Method Not Allowed", message, "METHOD_NOT_ALLOWED", request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * CRITICAL: Logs full stack trace but never exposes it in the response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                500, "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                "INTERNAL_ERROR",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
