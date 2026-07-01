package com.stormapi.scenario.execution;

import com.stormapi.collection.dto.KeyValuePairDto;
import com.stormapi.engine.http.HttpClientFactory;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.http.HttpRequestExecutor.DetailedRequestResult;
import com.stormapi.engine.http.RequestSpec;
import com.stormapi.scenario.dto.ExtractionRuleDto;
import com.stormapi.scenario.dto.ScenarioExecutionResponse;
import com.stormapi.scenario.dto.StepExecutionResult;
import com.stormapi.scenario.mapper.ScenarioMapper;
import com.stormapi.scenario.model.ScenarioStep;
import com.stormapi.scenario.model.TestScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates sequential execution of scenario steps with variable
 * extraction and template resolution between steps.
 *
 * <h3>Execution pipeline per step:</h3>
 * <ol>
 *   <li>Resolve {@code {{variable}}} placeholders in URL, headers, body</li>
 *   <li>Build {@link RequestSpec} from resolved values</li>
 *   <li>Execute HTTP request via {@link HttpRequestExecutor#executeWithBody}</li>
 *   <li>Extract variables from response body (if extraction rules defined)</li>
 *   <li>Merge extracted variables into shared variable store</li>
 *   <li>If step failed and failFast=true → abort remaining steps</li>
 * </ol>
 */
@Service
public class ScenarioExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScenarioExecutor.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final VariableExtractor variableExtractor;
    private final TemplateResolver templateResolver;

    public ScenarioExecutor() {
        this.variableExtractor = new VariableExtractor();
        this.templateResolver = new TemplateResolver();
    }

    // visible for testing
    ScenarioExecutor(VariableExtractor variableExtractor, TemplateResolver templateResolver) {
        this.variableExtractor = variableExtractor;
        this.templateResolver = templateResolver;
    }

    /**
     * Executes all steps of a scenario sequentially, chaining variable
     * extraction between steps.
     *
     * @param scenario the scenario to execute (with steps loaded)
     * @return execution response with results for each step
     */
    public ScenarioExecutionResponse execute(TestScenario scenario) {
        long startTimeMs = System.currentTimeMillis();

        var httpClient = HttpClientFactory.createDefault();
        var executor = new HttpRequestExecutor(httpClient);
        Map<String, String> variableStore = new LinkedHashMap<>();
        List<StepExecutionResult> stepResults = new ArrayList<>();

        List<ScenarioStep> steps = scenario.getSteps();
        if (steps == null || steps.isEmpty()) {
            return buildResponse(scenario, stepResults, System.currentTimeMillis() - startTimeMs);
        }

        log.info("Executing scenario '{}' with {} steps", scenario.getName(), steps.size());

        for (ScenarioStep step : steps) {
            StepExecutionResult result = executeStep(step, variableStore, executor);
            stepResults.add(result);

            if (!result.success() && scenario.isFailFast()) {
                log.info("Step {} '{}' failed, aborting scenario (failFast=true)",
                        step.getStepOrder(), step.getName());
                break;
            }
        }

        long totalDurationMs = System.currentTimeMillis() - startTimeMs;
        return buildResponse(scenario, stepResults, totalDurationMs);
    }

    /**
     * Executes a single scenario step with template resolution and variable extraction.
     */
    private StepExecutionResult executeStep(ScenarioStep step,
                                             Map<String, String> variableStore,
                                             HttpRequestExecutor executor) {
        log.debug("Executing step {} '{}'", step.getStepOrder(), step.getName());

        // 1. Resolve templates
        String resolvedUrl = templateResolver.resolve(step.getUrl(), variableStore);
        String resolvedBody = templateResolver.resolve(step.getBody(), variableStore);

        List<KeyValuePairDto> headerDtos = step.getHeaders() == null
                ? List.of()
                : step.getHeaders().stream()
                        .map(h -> new KeyValuePairDto(h.getKey(), h.getValue()))
                        .toList();
        Map<String, String> resolvedHeaders =
                templateResolver.resolveHeaders(headerDtos, variableStore);

        // 2. Build request spec
        RequestSpec spec;
        try {
            spec = new RequestSpec(
                    resolvedUrl,
                    step.getMethod().name(),
                    resolvedHeaders,
                    resolvedBody,
                    DEFAULT_TIMEOUT
            );
        } catch (IllegalArgumentException e) {
            return StepExecutionResult.failure(
                    step.getStepOrder(), step.getName(),
                    resolvedUrl, step.getMethod().name(),
                    0, 0, null, "Invalid request: " + e.getMessage());
        }

        // 3. Execute request
        DetailedRequestResult httpResult = executor.executeWithBody(spec);

        // 4. Extract variables if step succeeded and has extraction rules
        Map<String, String> extracted = Map.of();
        List<ExtractionRuleDto> rules =
                ScenarioMapper.parseExtractionRules(step.getExtractionRulesJson());

        if (httpResult.success() && !rules.isEmpty()) {
            extracted = variableExtractor.extract(httpResult.responseBody(), rules);
            variableStore.putAll(extracted);
            log.debug("Step {} extracted {} variables: {}",
                    step.getStepOrder(), extracted.size(), extracted.keySet());
        }

        // 5. Build result
        if (httpResult.success()) {
            return StepExecutionResult.success(
                    step.getStepOrder(), step.getName(),
                    resolvedUrl, step.getMethod().name(),
                    httpResult.statusCode(), httpResult.responseTimeMs(),
                    httpResult.responseBody(), extracted);
        } else {
            String errorMsg = httpResult.errorMessage() != null
                    ? httpResult.errorMessage()
                    : "HTTP " + httpResult.statusCode();
            return StepExecutionResult.failure(
                    step.getStepOrder(), step.getName(),
                    resolvedUrl, step.getMethod().name(),
                    httpResult.statusCode(), httpResult.responseTimeMs(),
                    httpResult.responseBody(), errorMsg);
        }
    }

    private ScenarioExecutionResponse buildResponse(TestScenario scenario,
                                                     List<StepExecutionResult> stepResults,
                                                     long totalDurationMs) {
        int completed = stepResults.size();
        int passed = (int) stepResults.stream().filter(StepExecutionResult::success).count();
        int failed = completed - passed;
        boolean success = failed == 0;

        return new ScenarioExecutionResponse(
                scenario.getId(),
                scenario.getName(),
                scenario.getSteps() != null ? scenario.getSteps().size() : 0,
                completed, passed, failed,
                totalDurationMs, success,
                stepResults
        );
    }

}
