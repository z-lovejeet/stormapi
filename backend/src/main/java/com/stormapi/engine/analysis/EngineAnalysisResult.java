package com.stormapi.engine.analysis;

import java.util.List;

/**
 * Immutable value object carrying engine-specific analysis results.
 *
 * Each advanced test engine sets this on the ExecutionContext before returning
 * from doExecute(). The TestOrchestrator reads it during result persistence
 * and maps the fields to TestResult columns.
 *
 * Uses nullable fields — each engine populates only its relevant fields.
 * The static factory methods enforce correct usage.
 */
public record EngineAnalysisResult(
        Integer breakpointUsers,        // BREAKPOINT only
        Long recoveryTimeMs,            // SPIKE only (-1 = never recovered)
        Double degradationSlope,        // SOAK only (ms/sec)
        Boolean degradationDetected,    // SOAK only
        List<ScalabilityPoint> scalabilityCurve  // SCALABILITY only
) {

    /**
     * A single data point on the scalability curve.
     * Represents throughput and latency at a given user count.
     */
    public record ScalabilityPoint(
            int users,
            double throughputRps,
            double avgLatencyMs,
            double errorRate
    ) {}

    // ── Static Factory Methods ──────────────────────────────────

    /** No analysis data — used by LoadTestEngine */
    public static EngineAnalysisResult none() {
        return new EngineAnalysisResult(null, null, null, null, null);
    }

    /** Breakpoint test result */
    public static EngineAnalysisResult breakpoint(int breakpointUsers) {
        return new EngineAnalysisResult(breakpointUsers, null, null, null, null);
    }

    /** Spike test result — recovery time in ms (-1 if never recovered) */
    public static EngineAnalysisResult spike(long recoveryTimeMs) {
        return new EngineAnalysisResult(null, recoveryTimeMs, null, null, null);
    }

    /** Soak test result — degradation slope and detection flag */
    public static EngineAnalysisResult soak(double degradationSlope, boolean degradationDetected) {
        return new EngineAnalysisResult(null, null, degradationSlope, degradationDetected, null);
    }

    /** Scalability test result — throughput curve data */
    public static EngineAnalysisResult scalability(List<ScalabilityPoint> curve) {
        return new EngineAnalysisResult(null, null, null, null,
                curve == null ? List.of() : List.copyOf(curve));
    }

}
