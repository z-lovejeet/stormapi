/**
 * Performance test types matching backend's TestType enum.
 */
export enum TestType {
  LOAD = 'LOAD',
  STRESS = 'STRESS',
  SPIKE = 'SPIKE',
  SOAK = 'SOAK',
  BREAKPOINT = 'BREAKPOINT',
  SCALABILITY = 'SCALABILITY',
}

/**
 * Test lifecycle states matching backend's TestStatus enum.
 */
export enum TestStatus {
  CREATED = 'CREATED',
  QUEUED = 'QUEUED',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

/**
 * HTTP methods matching backend's HttpMethod enum.
 */
export enum HttpMethod {
  GET = 'GET',
  POST = 'POST',
  PUT = 'PUT',
  DELETE = 'DELETE',
  PATCH = 'PATCH',
  HEAD = 'HEAD',
  OPTIONS = 'OPTIONS',
}

/**
 * Test configuration matching backend's TestConfig entity.
 */
export interface TestConfig {
  id: number;
  name: string;
  description?: string;
  targetUrl: string;
  httpMethod: HttpMethod;
  headers?: Record<string, string>;
  requestBody?: string;
  testType: TestType;
  virtualUsers: number;
  durationSeconds: number;
  rampUpSeconds: number;
  stepSize?: number;
  stepDurationSeconds?: number;
  spikeUsers?: number;
  maxRetries: number;
  timeoutMs: number;
  thinkTimeMs: number;
  status: TestStatus;
  createdAt: string;
  updatedAt: string;
}

/**
 * Test execution result matching backend's TestResult entity.
 */
export interface TestResult {
  id: number;
  testConfigId: number;
  status: TestStatus;
  totalRequests: number;
  successCount: number;
  failureCount: number;
  avgResponseTimeMs: number;
  minResponseTimeMs: number;
  maxResponseTimeMs: number;
  p50Ms: number;
  p75Ms: number;
  p90Ms: number;
  p95Ms: number;
  p99Ms: number;
  throughputRps: number;
  errorRate: number;
  totalDataBytes: number;
  startedAt: string;
  completedAt?: string;
  durationMs: number;
  breakpointUsers?: number;
  recoveryTimeMs?: number;
  degradationSlope?: number;
  degradationDetected?: boolean;
  createdAt: string;
  updatedAt: string;
}

// ── Request/Response DTOs ─────────────────────────────────

/**
 * Request DTO for POST /api/tests — matches backend CreateTestRequest.
 */
export interface CreateTestRequest {
  name: string;
  description?: string;
  targetUrl: string;
  httpMethod: HttpMethod;
  headers?: Record<string, string>;
  requestBody?: string;
  testType: TestType;
  virtualUsers: number;
  durationSeconds: number;
  rampUpSeconds: number;
  stepSize?: number;
  stepDurationSeconds?: number;
  spikeUsers?: number;
  maxRetries: number;
  timeoutMs: number;
  thinkTimeMs: number;
  autoStart: boolean;
}

/**
 * Full config response — matches backend TestConfigResponse.
 */
export interface TestConfigResponse {
  id: number;
  name: string;
  description?: string;
  targetUrl: string;
  httpMethod: HttpMethod;
  headers?: Record<string, string>;
  requestBody?: string;
  testType: TestType;
  virtualUsers: number;
  durationSeconds: number;
  rampUpSeconds: number;
  stepSize?: number;
  stepDurationSeconds?: number;
  spikeUsers?: number;
  maxRetries: number;
  timeoutMs: number;
  thinkTimeMs: number;
  status: TestStatus;
  createdAt: string;
  updatedAt: string;
}

/**
 * Lightweight summary for list views — matches backend TestSummaryResponse.
 */
export interface TestSummaryResponse {
  id: number;
  name: string;
  targetUrl: string;
  testType: TestType;
  status: TestStatus;
  virtualUsers: number;
  durationSeconds: number;
  lastRunAt?: string;
  lastAvgResponseTimeMs?: number;
  lastErrorRate?: number;
  totalRuns: number;
  createdAt: string;
}

/**
 * Full result response — matches backend TestResultResponse.
 */
export interface TestResultResponse {
  id: number;
  testConfigId: number;
  status: TestStatus;
  totalRequests: number;
  successCount: number;
  failureCount: number;
  avgResponseTimeMs: number;
  minResponseTimeMs: number;
  maxResponseTimeMs: number;
  p50Ms: number;
  p75Ms: number;
  p90Ms: number;
  p95Ms: number;
  p99Ms: number;
  throughputRps: number;
  errorRate: number;
  totalDataBytes: number;
  startedAt: string;
  completedAt?: string;
  durationMs: number;
  breakpointUsers?: number;
  recoveryTimeMs?: number;
  degradationSlope?: number;
  degradationDetected?: boolean;
  createdAt: string;
}
