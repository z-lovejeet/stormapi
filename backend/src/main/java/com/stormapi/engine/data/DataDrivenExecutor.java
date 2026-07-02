package com.stormapi.engine.data;

import com.stormapi.engine.assertion.Assertion;
import com.stormapi.engine.assertion.AssertionEvaluator;
import com.stormapi.scenario.dto.ScenarioExecutionResponse;
import com.stormapi.scenario.execution.ScenarioExecutor;
import com.stormapi.scenario.model.ScenarioStep;
import com.stormapi.scenario.model.TestScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executes a scenario once per data row, merging row variables into
 * each step's template context before execution.
 *
 * <p>For each row:
 * <ol>
 *   <li>Injects row key-value pairs as template variables</li>
 *   <li>Delegates execution to {@link ScenarioExecutor}</li>
 *   <li>Captures per-row pass/fail based on step outcomes</li>
 * </ol>
 *
 * <p>Row variables are injected by modifying the step URLs, headers,
 * and body text — replacing {@code {{columnName}}} with the row's value
 * before the scenario executor does its own template resolution.
 */
@Service
public class DataDrivenExecutor {

    private static final Logger log = LoggerFactory.getLogger(DataDrivenExecutor.class);

    private final ScenarioExecutor scenarioExecutor;

    public DataDrivenExecutor(ScenarioExecutor scenarioExecutor) {
        this.scenarioExecutor = scenarioExecutor;
    }

    /**
     * Executes the scenario once per data row.
     *
     * @param scenario   the scenario to execute
     * @param rows       parsed data rows (each row = variable map)
     * @param assertions assertions to evaluate per step (may be empty)
     * @return aggregate execution response with per-row results
     */
    public DataDrivenExecutionResponse execute(TestScenario scenario,
                                                List<Map<String, String>> rows,
                                                List<Assertion> assertions) {
        long startTime = System.currentTimeMillis();
        List<DataRowResult> rowResults = new ArrayList<>(rows.size());

        log.info("Starting data-driven execution of '{}' with {} rows",
                scenario.getName(), rows.size());

        int passedCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> rowData = rows.get(i);
            log.debug("Executing row {} with data: {}", i, rowData);

            // Create a scenario copy with row variables injected into step templates
            TestScenario parameterizedScenario = injectRowVariables(scenario, rowData);

            // Execute the parameterized scenario
            ScenarioExecutionResponse rowResponse =
                    scenarioExecutor.execute(parameterizedScenario, assertions);

            boolean allPassed = rowResponse.stepResults().stream()
                    .allMatch(step -> step.success() &&
                            step.assertionResults().stream()
                                    .allMatch(ar -> ar.passed()));

            if (allPassed) {
                passedCount++;
            }

            rowResults.add(new DataRowResult(i, rowData, rowResponse, allPassed));
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        log.info("Data-driven execution complete: {}/{} rows passed in {}ms",
                passedCount, rows.size(), totalDuration);

        return new DataDrivenExecutionResponse(
                scenario.getName(),
                rows.size(),
                passedCount,
                rows.size() - passedCount,
                totalDuration,
                rowResults
        );
    }

    /**
     * Creates a shallow copy of the scenario with row variables injected
     * into step URLs, headers, and body by simple string replacement.
     *
     * <p>This pre-injection allows row data to be merged before the
     * executor's own template resolution (which handles extraction variables).
     */
    private TestScenario injectRowVariables(TestScenario original, Map<String, String> rowData) {
        // Create a copy of the scenario with modified steps
        TestScenario copy = TestScenario.builder()
                .name(original.getName())
                .description(original.getDescription())
                .failFast(original.isFailFast())
                .build();
        copy.setId(original.getId());

        List<ScenarioStep> modifiedSteps = new ArrayList<>();
        for (ScenarioStep step : original.getSteps()) {
            ScenarioStep modified = ScenarioStep.builder()
                    .scenario(copy)
                    .stepOrder(step.getStepOrder())
                    .name(step.getName())
                    .url(replaceVariables(step.getUrl(), rowData))
                    .method(step.getMethod())
                    .headers(step.getHeaders()) // headers are resolved by TemplateResolver
                    .body(replaceVariables(step.getBody(), rowData))
                    .extractionRulesJson(step.getExtractionRulesJson())
                    .build();
            modifiedSteps.add(modified);
        }

        copy.setSteps(modifiedSteps);
        return copy;
    }

    /**
     * Replaces {{key}} placeholders in template with values from the row data map.
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
