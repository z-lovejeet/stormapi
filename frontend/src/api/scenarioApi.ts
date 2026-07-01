import { apiClient } from './client';
import type {
  TestScenario,
  ScenarioStep,
  ScenarioExecutionResponse,
  ExtractionRule,
} from '../types/scenario';
import type { HttpMethod } from '../types/test';
import type { KeyValuePair } from '../types/collection';

/**
 * Scenario CRUD + execution API module.
 */

// ── Scenario-level ────────────────────────────────────────

export async function createScenario(req: {
  name: string;
  description?: string;
  failFast?: boolean;
}): Promise<TestScenario> {
  const { data } = await apiClient.post<TestScenario>('/scenarios', req);
  return data;
}

export async function listScenarios(): Promise<TestScenario[]> {
  const { data } = await apiClient.get<TestScenario[]>('/scenarios');
  return data;
}

export async function getScenario(id: number): Promise<TestScenario> {
  const { data } = await apiClient.get<TestScenario>(`/scenarios/${id}`);
  return data;
}

export async function updateScenario(
  id: number,
  req: { name: string; description?: string; failFast?: boolean },
): Promise<TestScenario> {
  const { data } = await apiClient.put<TestScenario>(`/scenarios/${id}`, req);
  return data;
}

export async function deleteScenario(id: number): Promise<void> {
  await apiClient.delete(`/scenarios/${id}`);
}

// ── Step-level ────────────────────────────────────────────

export async function addStep(
  scenarioId: number,
  req: {
    name: string;
    url: string;
    method: HttpMethod;
    headers?: KeyValuePair[];
    body?: string;
    extractionRules?: ExtractionRule[];
  },
): Promise<ScenarioStep> {
  const { data } = await apiClient.post<ScenarioStep>(
    `/scenarios/${scenarioId}/steps`,
    req,
  );
  return data;
}

export async function updateStep(
  scenarioId: number,
  stepId: number,
  req: {
    name: string;
    url: string;
    method: HttpMethod;
    headers?: KeyValuePair[];
    body?: string;
    extractionRules?: ExtractionRule[];
  },
): Promise<ScenarioStep> {
  const { data } = await apiClient.put<ScenarioStep>(
    `/scenarios/${scenarioId}/steps/${stepId}`,
    req,
  );
  return data;
}

export async function deleteStep(
  scenarioId: number,
  stepId: number,
): Promise<void> {
  await apiClient.delete(`/scenarios/${scenarioId}/steps/${stepId}`);
}

export async function reorderSteps(
  scenarioId: number,
  stepIds: number[],
): Promise<ScenarioStep[]> {
  const { data } = await apiClient.put<ScenarioStep[]>(
    `/scenarios/${scenarioId}/steps/reorder`,
    { stepIds },
  );
  return data;
}

// ── Execution ─────────────────────────────────────────────

export async function executeScenario(
  id: number,
): Promise<ScenarioExecutionResponse> {
  const { data } = await apiClient.post<ScenarioExecutionResponse>(
    `/scenarios/${id}/execute`,
  );
  return data;
}
