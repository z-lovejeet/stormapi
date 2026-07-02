package com.stormapi.engine.assertion;

/**
 * Immutable result of evaluating a single assertion against an HTTP response.
 *
 * <p>Every assertion evaluation produces exactly one result — never null, never throws.
 * Failed evaluations (e.g., null body for JSONPath) produce {@code passed=false}
 * with an explanatory message.
 *
 * @param passed         whether the assertion condition was satisfied
 * @param assertionType  type identifier (STATUS_CODE, RESPONSE_TIME, etc.)
 * @param target         what was tested (e.g., "statusCode", "$.data.id", "Content-Type")
 * @param expected       the expected value or condition (e.g., "200", "<500ms")
 * @param actual         the actual observed value (e.g., "404", "342.5ms")
 * @param message        human-readable summary of the result
 */
public record AssertionResult(
        boolean passed,
        String assertionType,
        String target,
        String expected,
        String actual,
        String message
) {

    /**
     * Factory for a passing assertion.
     */
    public static AssertionResult pass(String type, String target,
                                        String expected, String actual, String message) {
        return new AssertionResult(true, type, target, expected, actual, message);
    }

    /**
     * Factory for a failing assertion.
     */
    public static AssertionResult fail(String type, String target,
                                        String expected, String actual, String message) {
        return new AssertionResult(false, type, target, expected, actual, message);
    }

    /**
     * Factory for an assertion that errored during evaluation.
     */
    public static AssertionResult error(String type, String errorMessage) {
        return new AssertionResult(false, type, "N/A", "N/A", "N/A",
                "Assertion error: " + errorMessage);
    }
}
