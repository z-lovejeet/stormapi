package com.stormapi.engine.assertion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssertionEvaluatorTest {

    private final AssertionEvaluator evaluator = new AssertionEvaluator();

    @Test
    void shouldReturnAllPassingResults() {
        List<Assertion> assertions = List.of(
                new StatusCodeAssertion(200),
                new ResponseTimeAssertion(500.0)
        );
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}", Map.of());

        List<AssertionResult> results = evaluator.evaluate(assertions, ctx);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(AssertionResult::passed));
    }

    @Test
    void shouldReturnMixedResults() {
        List<Assertion> assertions = List.of(
                new StatusCodeAssertion(200),
                new ResponseTimeAssertion(50.0)  // will fail — 100ms > 50ms
        );
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}", Map.of());

        List<AssertionResult> results = evaluator.evaluate(assertions, ctx);

        assertEquals(2, results.size());
        assertTrue(results.get(0).passed());   // status code passes
        assertFalse(results.get(1).passed());  // response time fails
    }

    @Test
    void shouldHandleExceptionInAssertion() {
        Assertion throwingAssertion = new Assertion() {
            @Override
            public AssertionResult evaluate(AssertionContext context) {
                throw new RuntimeException("boom");
            }
            @Override
            public String getType() { return "THROW"; }
        };

        List<Assertion> assertions = List.of(
                new StatusCodeAssertion(200),
                throwingAssertion
        );
        AssertionContext ctx = new AssertionContext(200, 100.0, "{}", Map.of());

        List<AssertionResult> results = evaluator.evaluate(assertions, ctx);

        assertEquals(2, results.size());
        assertTrue(results.get(0).passed());
        assertFalse(results.get(1).passed());
        assertTrue(results.get(1).message().contains("boom"));
    }

    @Test
    void shouldReturnEmptyListForEmptyInput() {
        List<AssertionResult> results = evaluator.evaluate(List.of(),
                new AssertionContext(200, 100.0, "{}", Map.of()));
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForNullInput() {
        List<AssertionResult> results = evaluator.evaluate(null,
                new AssertionContext(200, 100.0, "{}", Map.of()));
        assertTrue(results.isEmpty());
    }

    // --- Factory method tests ---

    @Test
    void shouldCreateStatusCodeAssertion() {
        AssertionDefinition def = new AssertionDefinition("STATUS_CODE", null, "EQUALS", "200");
        Assertion assertion = AssertionEvaluator.createAssertion(def);

        assertNotNull(assertion);
        assertEquals("STATUS_CODE", assertion.getType());
    }

    @Test
    void shouldCreateResponseTimeAssertion() {
        AssertionDefinition def = new AssertionDefinition("RESPONSE_TIME", null, "LESS_THAN", "500");
        Assertion assertion = AssertionEvaluator.createAssertion(def);

        assertNotNull(assertion);
        assertEquals("RESPONSE_TIME", assertion.getType());
    }

    @Test
    void shouldCreateBodyContainsAssertion() {
        AssertionDefinition def = new AssertionDefinition("BODY_CONTAINS", null, "CONTAINS", "success");
        Assertion assertion = AssertionEvaluator.createAssertion(def);

        assertNotNull(assertion);
        assertEquals("BODY_CONTAINS", assertion.getType());
    }

    @Test
    void shouldCreateJsonPathAssertion() {
        AssertionDefinition def = new AssertionDefinition("JSON_PATH", "$.data.id", "EQUALS", "42");
        Assertion assertion = AssertionEvaluator.createAssertion(def);

        assertNotNull(assertion);
        assertEquals("JSON_PATH", assertion.getType());
    }

    @Test
    void shouldCreateHeaderAssertion() {
        AssertionDefinition def = new AssertionDefinition("HEADER", "Content-Type", "EQUALS", "application/json");
        Assertion assertion = AssertionEvaluator.createAssertion(def);

        assertNotNull(assertion);
        assertEquals("HEADER", assertion.getType());
    }

    @Test
    void shouldThrowForUnknownType() {
        AssertionDefinition def = new AssertionDefinition("UNKNOWN", null, null, "value");
        assertThrows(IllegalArgumentException.class, () -> AssertionEvaluator.createAssertion(def));
    }

    @Test
    void shouldCreateMultipleAssertions() {
        List<AssertionDefinition> defs = List.of(
                new AssertionDefinition("STATUS_CODE", null, "EQUALS", "200"),
                new AssertionDefinition("RESPONSE_TIME", null, "LESS_THAN", "1000")
        );

        List<Assertion> assertions = AssertionEvaluator.createAssertions(defs);

        assertEquals(2, assertions.size());
    }

    @Test
    void shouldReturnEmptyForNullDefinitions() {
        List<Assertion> assertions = AssertionEvaluator.createAssertions(null);
        assertTrue(assertions.isEmpty());
    }
}
