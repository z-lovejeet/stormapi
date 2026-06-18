package com.stormapi.engine;

import com.stormapi.engine.load.LoadTestEngine;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestEngineFactory Tests")
class TestEngineFactoryTest {

    @Test
    @DisplayName("create(LOAD) returns LoadTestEngine")
    void create_load_returnsLoadTestEngine() {
        TestEngine engine = TestEngineFactory.create(TestType.LOAD);
        assertNotNull(engine);
        assertInstanceOf(LoadTestEngine.class, engine);
        assertEquals(TestType.LOAD, engine.getSupportedType());
    }

    @Test
    @DisplayName("create(STRESS) throws UnsupportedOperationException")
    void create_stress_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> TestEngineFactory.create(TestType.STRESS));
    }

    @Test
    @DisplayName("create(SPIKE) throws UnsupportedOperationException")
    void create_spike_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> TestEngineFactory.create(TestType.SPIKE));
    }

    @Test
    @DisplayName("create(SOAK) throws UnsupportedOperationException")
    void create_soak_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> TestEngineFactory.create(TestType.SOAK));
    }

    @Test
    @DisplayName("create(BREAKPOINT) throws UnsupportedOperationException")
    void create_breakpoint_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> TestEngineFactory.create(TestType.BREAKPOINT));
    }

    @Test
    @DisplayName("create(SCALABILITY) throws UnsupportedOperationException")
    void create_scalability_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> TestEngineFactory.create(TestType.SCALABILITY));
    }

}
