package com.stormapi.engine.data;

import java.util.List;

/**
 * Response DTO for a complete data-driven test execution.
 *
 * @param scenarioName  name of the executed scenario
 * @param totalRows     total number of data rows processed
 * @param passedRows    number of rows where all steps/assertions passed
 * @param failedRows    number of rows with at least one failure
 * @param totalDurationMs total wall-clock time for the entire execution
 * @param rowResults    per-row execution details
 */
public record DataDrivenExecutionResponse(
        String scenarioName,
        int totalRows,
        int passedRows,
        int failedRows,
        long totalDurationMs,
        List<DataRowResult> rowResults
) {
}
