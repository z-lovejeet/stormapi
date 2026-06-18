package com.stormapi.engine;

import com.stormapi.engine.breakpoint.BreakpointTestEngine;
import com.stormapi.engine.load.LoadTestEngine;
import com.stormapi.engine.scalability.ScalabilityTestEngine;
import com.stormapi.engine.soak.SoakTestEngine;
import com.stormapi.engine.spike.SpikeTestEngine;
import com.stormapi.engine.stress.StressTestEngine;
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
    @DisplayName("create(STRESS) returns StressTestEngine")
    void create_stress_returnsStressTestEngine() {
        TestEngine engine = TestEngineFactory.create(TestType.STRESS);
        assertNotNull(engine);
        assertInstanceOf(StressTestEngine.class, engine);
        assertEquals(TestType.STRESS, engine.getSupportedType());
    }

    @Test
    @DisplayName("create(SPIKE) returns SpikeTestEngine")
    void create_spike_returnsSpikeTestEngine() {
        TestEngine engine = TestEngineFactory.create(TestType.SPIKE);
        assertNotNull(engine);
        assertInstanceOf(SpikeTestEngine.class, engine);
        assertEquals(TestType.SPIKE, engine.getSupportedType());
    }

    @Test
    @DisplayName("create(SOAK) returns SoakTestEngine")
    void create_soak_returnsSoakTestEngine() {
        TestEngine engine = TestEngineFactory.create(TestType.SOAK);
        assertNotNull(engine);
        assertInstanceOf(SoakTestEngine.class, engine);
        assertEquals(TestType.SOAK, engine.getSupportedType());
    }

    @Test
    @DisplayName("create(BREAKPOINT) returns BreakpointTestEngine")
    void create_breakpoint_returnsBreakpointTestEngine() {
        TestEngine engine = TestEngineFactory.create(TestType.BREAKPOINT);
        assertNotNull(engine);
        assertInstanceOf(BreakpointTestEngine.class, engine);
        assertEquals(TestType.BREAKPOINT, engine.getSupportedType());
    }

    @Test
    @DisplayName("create(SCALABILITY) returns ScalabilityTestEngine")
    void create_scalability_returnsScalabilityTestEngine() {
        TestEngine engine = TestEngineFactory.create(TestType.SCALABILITY);
        assertNotNull(engine);
        assertInstanceOf(ScalabilityTestEngine.class, engine);
        assertEquals(TestType.SCALABILITY, engine.getSupportedType());
    }

}
