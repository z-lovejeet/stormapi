package com.stormapi.engine.assertion;

/**
 * Asserts that the HTTP response status code equals the expected value.
 *
 * <p>Example: expected=200 → passes if response is 200, fails if 404.
 */
public class StatusCodeAssertion implements Assertion {

    private static final String TYPE = "STATUS_CODE";

    private final int expectedStatusCode;

    public StatusCodeAssertion(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    @Override
    public AssertionResult evaluate(AssertionContext context) {
        int actual = context.statusCode();
        boolean passed = actual == expectedStatusCode;

        String message = passed
                ? "Status code " + actual + " matches expected " + expectedStatusCode
                : "Expected status " + expectedStatusCode + " but got " + actual;

        return passed
                ? AssertionResult.pass(TYPE, "statusCode",
                        String.valueOf(expectedStatusCode), String.valueOf(actual), message)
                : AssertionResult.fail(TYPE, "statusCode",
                        String.valueOf(expectedStatusCode), String.valueOf(actual), message);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
