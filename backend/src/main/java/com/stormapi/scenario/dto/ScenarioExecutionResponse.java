package com.stormapi.scenario.dto;

import java.util.List;

/**
 * Complete result of executing a test scenario.
 * Aggregates results from all steps with summary statistics.
 */
public record ScenarioExecutionResponse(
        Long scenarioId,
        String scenarioName,
        int totalSteps,
        int completedSteps,
        int passedSteps,
        int failedSteps,
        long totalDurationMs,
        boolean success,
        List<StepExecutionResult> stepResults
) {}
