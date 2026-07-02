package com.stormapi.engine.assertion;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatusCodeAssertionTest {

    private final StatusCodeAssertion assertion200 = new StatusCodeAssertion(200);
    private final StatusCodeAssertion assertion201 = new StatusCodeAssertion(201);

    @Test
    void shouldPassWhenStatusCodeMatches() {
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}", Map.of());
        AssertionResult result = assertion200.evaluate(ctx);

        assertTrue(result.passed());
        assertEquals("STATUS_CODE", result.assertionType());
        assertEquals("200", result.expected());
        assertEquals("200", result.actual());
    }

    @Test
    void shouldFailWhenStatusCodeDoesNotMatch() {
        AssertionContext ctx = new AssertionContext(404, 100.0, "{}", Map.of());
        AssertionResult result = assertion200.evaluate(ctx);

        assertFalse(result.passed());
        assertEquals("200", result.expected());
        assertEquals("404", result.actual());
        assertTrue(result.message().contains("404"));
    }

    @Test
    void shouldHandleZeroStatusCode() {
        AssertionContext ctx = new AssertionContext(0, 100.0, null, Map.of());
        AssertionResult result = assertion200.evaluate(ctx);

        assertFalse(result.passed());
        assertEquals("0", result.actual());
    }
}
