package com.stormapi.engine.assertion;

/**
 * Asserts that the HTTP response time is less than the specified maximum.
 *
 * <p>Example: maxMs=500 → passes if response took 342ms, fails if 612ms.
 */
public class ResponseTimeAssertion implements Assertion {

    private static final String TYPE = "RESPONSE_TIME";

    private final double maxMs;

    public ResponseTimeAssertion(double maxMs) {
        if (maxMs <= 0) {
            throw new IllegalArgumentException("Maximum response time must be positive, got: " + maxMs);
        }
        this.maxMs = maxMs;
    }

    @Override
    public AssertionResult evaluate(AssertionContext context) {
        double actual = context.responseTimeMs();
        boolean passed = actual < maxMs;

        String expectedStr = "<" + formatMs(maxMs);
        String actualStr = formatMs(actual);
        String message = passed
                ? "Response time " + actualStr + " is under " + formatMs(maxMs) + " limit"
                : "Response time " + actualStr + " exceeds " + formatMs(maxMs) + " limit";

        return passed
                ? AssertionResult.pass(TYPE, "responseTime", expectedStr, actualStr, message)
                : AssertionResult.fail(TYPE, "responseTime", expectedStr, actualStr, message);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    private static String formatMs(double ms) {
        return String.format("%.1fms", ms);
    }
}
