package com.stormapi.engine.assertion;

/**
 * Strategy interface for response assertions.
 *
 * <p>Each implementation validates one aspect of an HTTP response
 * (status code, response time, body content, JSON path, or header).
 *
 * <p>Contract:
 * <ul>
 *   <li>Implementations MUST be stateless and thread-safe</li>
 *   <li>{@link #evaluate} MUST NOT throw — return a failure result instead</li>
 *   <li>{@link #getType} returns a stable string identifier for serialization</li>
 * </ul>
 *
 * <p>Adding a new assertion type requires only a new implementation of this
 * interface plus a case in {@link AssertionEvaluator#createAssertion}.
 */
public interface Assertion {

    /**
     * Evaluates this assertion against the given response context.
     *
     * @param context immutable snapshot of the HTTP response data
     * @return result indicating pass/fail with human-readable message
     */
    AssertionResult evaluate(AssertionContext context);

    /**
     * Returns the type identifier for this assertion.
     * Must match one of: STATUS_CODE, RESPONSE_TIME, BODY_CONTAINS, JSON_PATH, HEADER.
     */
    String getType();
}
