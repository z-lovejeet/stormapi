package com.stormapi.engine.assertion;

import java.util.Map;

/**
 * Asserts that a specific HTTP response header has the expected value.
 *
 * <p>Header name comparison is case-insensitive (per HTTP/1.1 spec).
 * Header value comparison is case-sensitive.
 */
public class HeaderAssertion implements Assertion {

    private static final String TYPE = "HEADER";

    private final String headerName;
    private final String expectedValue;

    public HeaderAssertion(String headerName, String expectedValue) {
        if (headerName == null || headerName.isBlank()) {
            throw new IllegalArgumentException("Header name must not be blank");
        }
        this.headerName = headerName;
        this.expectedValue = expectedValue != null ? expectedValue : "";
    }

    @Override
    public AssertionResult evaluate(AssertionContext context) {
        Map<String, String> headers = context.responseHeaders();

        if (headers == null || headers.isEmpty()) {
            return AssertionResult.fail(TYPE, headerName,
                    expectedValue, "(no headers)",
                    "Response has no headers, cannot check '" + headerName + "'");
        }

        // Case-insensitive header name lookup
        String actualValue = null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                actualValue = entry.getValue();
                break;
            }
        }

        if (actualValue == null) {
            return AssertionResult.fail(TYPE, headerName,
                    expectedValue, "(not present)",
                    "Header '" + headerName + "' not found in response");
        }

        boolean passed = expectedValue.equals(actualValue);
        String message = passed
                ? "Header '" + headerName + "' value '" + actualValue + "' matches expected"
                : "Header '" + headerName + "' value '" + actualValue + "' does not match expected '" + expectedValue + "'";

        return passed
                ? AssertionResult.pass(TYPE, headerName, expectedValue, actualValue, message)
                : AssertionResult.fail(TYPE, headerName, expectedValue, actualValue, message);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
