package com.stormapi.test.model;

/**
 * Defines the 6 types of performance tests StormAPI supports.
 * Each value maps to a different TestEngine implementation in Phase 6–7.
 */
public enum TestType {
    LOAD,
    STRESS,
    SPIKE,
    SOAK,
    BREAKPOINT,
    SCALABILITY
}
