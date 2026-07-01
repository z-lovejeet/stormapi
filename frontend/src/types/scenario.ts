import { HttpMethod } from './test';
import { KeyValuePair } from './collection';

// ── Extraction Rules ────────────────────────────────────────

/** Single variable extraction rule mapping JSONPath to a named variable. */
export interface ExtractionRule {
  variableName: string;
  jsonPath: string;
}

// ── Scenario Step ───────────────────────────────────────────

/** A single step within a test scenario. */
export interface ScenarioStep {
  id: number;
  stepOrder: number;
  name: string;
  url: string;
  method: HttpMethod;
  headers: KeyValuePair[];
  body?: string;
  extractionRules: ExtractionRule[];
}

// ── Test Scenario ───────────────────────────────────────────

/** Multi-step test scenario definition. */
export interface TestScenario {
  id: number;
  name: string;
  description?: string;
  failFast: boolean;
  steps: ScenarioStep[];
  createdAt: string;
  updatedAt: string;
}

// ── Execution Results ───────────────────────────────────────

/** Result of executing a single scenario step. */
export interface StepExecutionResult {
  stepOrder: number;
  stepName: string;
  url: string;
  method: string;
  statusCode: number;
  responseTimeMs: number;
  responseBodyPreview: string;
  success: boolean;
  errorMessage?: string;
  extractedVariables: Record<string, string>;
}

/** Complete result of executing a test scenario. */
export interface ScenarioExecutionResponse {
  scenarioId: number;
  scenarioName: string;
  totalSteps: number;
  completedSteps: number;
  passedSteps: number;
  failedSteps: number;
  totalDurationMs: number;
  success: boolean;
  stepResults: StepExecutionResult[];
}
