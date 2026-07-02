package com.stormapi.engine.assertion;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTimeAssertionTest {

    @Test
    void shouldPassWhenUnderLimit() {
        ResponseTimeAssertion assertion = new ResponseTimeAssertion(500.0);
        AssertionContext ctx = new AssertionContext(200, 342.5, "{}", Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertTrue(result.passed());
        assertEquals("RESPONSE_TIME", result.assertionType());
        assertTrue(result.message().contains("under"));
    }

    @Test
    void shouldFailWhenOverLimit() {
        ResponseTimeAssertion assertion = new ResponseTimeAssertion(200.0);
        AssertionContext ctx = new AssertionContext(200, 612.3, "{}", Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
        assertTrue(result.message().contains("exceeds"));
    }

    @Test
    void shouldFailAtExactBoundary() {
        // Boundary: exact match should fail (strict less-than)
        ResponseTimeAssertion assertion = new ResponseTimeAssertion(500.0);
        AssertionContext ctx = new AssertionContext(200, 500.0, "{}", Map.of());
        AssertionResult result = assertion.evaluate(ctx);

        assertFalse(result.passed());
    }

    @Test
    void shouldRejectNonPositiveMax() {
        assertThrows(IllegalArgumentException.class, () -> new ResponseTimeAssertion(0));
        assertThrows(IllegalArgumentException.class, () -> new ResponseTimeAssertion(-100));
    }
}
