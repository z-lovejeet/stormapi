// ── Assertion Types ──────────────────────────────────────────

/** Supported assertion types. */
export type AssertionType =
  | 'STATUS_CODE'
  | 'RESPONSE_TIME'
  | 'BODY_CONTAINS'
  | 'JSON_PATH'
  | 'HEADER';

/** Definition of a single assertion sent to the backend. */
export interface AssertionDefinition {
  type: AssertionType;
  target?: string;
  operator?: string;
  expectedValue: string;
}

/** Result of evaluating a single assertion. */
export interface AssertionResult {
  passed: boolean;
  assertionType: string;
  target: string;
  expected: string;
  actual: string;
  message: string;
}

// ── Data-Driven Types ───────────────────────────────────────

/** Supported data formats. */
export type DataFormat = 'CSV' | 'JSON';

/** Request to execute a data-driven test. */
export interface DataDrivenRequest {
  scenarioId: number;
  format: DataFormat;
  dataContent: string;
  assertions?: AssertionDefinition[];
}

/** Result of a single data row execution. */
export interface DataRowResult {
  rowIndex: number;
  rowData: Record<string, string>;
  result: import('./scenario').ScenarioExecutionResponse;
  allPassed: boolean;
}

/** Complete response from data-driven execution. */
export interface DataDrivenExecutionResponse {
  scenarioName: string;
  totalRows: number;
  passedRows: number;
  failedRows: number;
  totalDurationMs: number;
  rowResults: DataRowResult[];
}
