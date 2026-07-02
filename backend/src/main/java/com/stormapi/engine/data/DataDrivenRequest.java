package com.stormapi.engine.data;

import com.stormapi.engine.assertion.AssertionDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for data-driven test execution.
 *
 * @param scenarioId  ID of the scenario to execute for each data row
 * @param format      data format: "CSV" or "JSON"
 * @param dataContent raw data content (CSV text or JSON array)
 * @param assertions  optional assertion definitions to evaluate per row
 */
public record DataDrivenRequest(
        @NotNull Long scenarioId,
        @NotBlank String format,
        @NotBlank String dataContent,
        List<AssertionDefinition> assertions
) {
}
