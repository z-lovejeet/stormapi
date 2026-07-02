package com.stormapi.engine.assertion;

/**
 * Asserts that the HTTP response body contains the expected text.
 *
 * <p>Supports case-sensitive and case-insensitive matching.
 * Returns a failure result (not an exception) if the body is null.
 */
public class BodyContainsAssertion implements Assertion {

    private static final String TYPE = "BODY_CONTAINS";

    private final String expectedText;
    private final boolean caseSensitive;

    public BodyContainsAssertion(String expectedText, boolean caseSensitive) {
        if (expectedText == null || expectedText.isBlank()) {
            throw new IllegalArgumentException("Expected text must not be blank");
        }
        this.expectedText = expectedText;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public AssertionResult evaluate(AssertionContext context) {
        String body = context.responseBody();

        if (body == null || body.isEmpty()) {
            return AssertionResult.fail(TYPE, "body",
                    "contains '" + expectedText + "'", "(empty body)",
                    "Response body is empty, cannot check for '" + expectedText + "'");
        }

        boolean found;
        if (caseSensitive) {
            found = body.contains(expectedText);
        } else {
            found = body.toLowerCase().contains(expectedText.toLowerCase());
        }

        String actualPreview = body.length() > 100
                ? body.substring(0, 100) + "..."
                : body;

        if (found) {
            return AssertionResult.pass(TYPE, "body",
                    "contains '" + expectedText + "'", "found",
                    "Response body contains '" + expectedText + "'");
        } else {
            return AssertionResult.fail(TYPE, "body",
                    "contains '" + expectedText + "'", "not found",
                    "Response body does not contain '" + expectedText + "'");
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
