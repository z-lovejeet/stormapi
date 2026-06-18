package com.stormapi.engine;

import com.stormapi.engine.breakpoint.BreakpointTestEngine;
import com.stormapi.engine.load.LoadTestEngine;
import com.stormapi.engine.scalability.ScalabilityTestEngine;
import com.stormapi.engine.soak.SoakTestEngine;
import com.stormapi.engine.spike.SpikeTestEngine;
import com.stormapi.engine.stress.StressTestEngine;
import com.stormapi.test.model.TestType;

/**
 * Creates the correct {@link TestEngine} for a given {@link TestType}.
 *
 * Decouples engine construction from the orchestrator. Adding a new
 * test type engine (Phase 7) only requires adding a case here.
 *
 * NOT a Spring bean — engines are plain objects, instantiated per test run.
 */
public final class TestEngineFactory {

    private TestEngineFactory() {
        // utility class
    }

    /**
     * Creates a test engine matching the given test type.
     *
     * @param type the test type from TestConfig
     * @return a new engine instance
     * @throws UnsupportedOperationException if the test type is not yet implemented
     */
    public static TestEngine create(TestType type) {
        return switch (type) {
            case LOAD        -> new LoadTestEngine();
            case STRESS      -> new StressTestEngine();
            case SPIKE       -> new SpikeTestEngine();
            case SOAK        -> new SoakTestEngine();
            case BREAKPOINT  -> new BreakpointTestEngine();
            case SCALABILITY -> new ScalabilityTestEngine();
        };
    }

}
