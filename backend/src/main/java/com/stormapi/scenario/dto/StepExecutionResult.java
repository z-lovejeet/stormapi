package com.stormapi.scenario.dto;

import java.util.Map;

/**
 * Result of executing a single scenario step.
 * Includes HTTP response details and any extracted variables.
 */
public record StepExecutionResult(
        int stepOrder,
        String stepName,
        String url,
        String method,
        int statusCode,
        double responseTimeMs,
        String responseBodyPreview,
        boolean success,
        String errorMessage,
        Map<String, String> extractedVariables
) {

    /** Maximum characters to retain from response body for preview. */
    private static final int MAX_PREVIEW_LENGTH = 500;

    /**
     * Factory for a successful step execution.
     */
    public static StepExecutionResult success(int stepOrder, String stepName,
                                               String url, String method,
                                               int statusCode, double responseTimeMs,
                                               String responseBody,
                                               Map<String, String> extractedVariables) {
        return new StepExecutionResult(
                stepOrder, stepName, url, method,
                statusCode, responseTimeMs,
                truncate(responseBody),
                true, null, extractedVariables
        );
    }

    /**
     * Factory for a failed step execution.
     */
    public static StepExecutionResult failure(int stepOrder, String stepName,
                                               String url, String method,
                                               int statusCode, double responseTimeMs,
                                               String responseBody, String errorMessage) {
        return new StepExecutionResult(
                stepOrder, stepName, url, method,
                statusCode, responseTimeMs,
                truncate(responseBody),
                false, errorMessage, Map.of()
        );
    }

    private static String truncate(String body) {
        if (body == null) return "";
        return body.length() > MAX_PREVIEW_LENGTH
                ? body.substring(0, MAX_PREVIEW_LENGTH) + "..."
                : body;
    }

}
