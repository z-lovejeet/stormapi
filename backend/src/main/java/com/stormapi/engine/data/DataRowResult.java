package com.stormapi.engine.data;

import com.stormapi.scenario.dto.ScenarioExecutionResponse;

import java.util.Map;

/**
 * Result of executing one scenario run for a single data row.
 *
 * @param rowIndex     0-based index of the data row
 * @param rowData      the variable values from this data row
 * @param result       full scenario execution response for this row
 * @param allPassed    true if all steps and assertions passed
 */
public record DataRowResult(
        int rowIndex,
        Map<String, String> rowData,
        ScenarioExecutionResponse result,
        boolean allPassed
) {
}
