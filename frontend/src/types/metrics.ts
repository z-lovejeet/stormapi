/**
 * Time-series metric snapshot matching backend's MetricSnapshot entity.
 * One snapshot per second during test execution.
 */
export interface MetricSnapshot {
  id: number;
  testResultId: number;
  timestamp: string;
  activeUsers: number;
  requestsPerSecond: number;
  avgResponseTimeMs: number;
  errorRate: number;
  p95Ms: number;
  cumulativeRequests: number;
  cumulativeErrors: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Individual request log entry matching backend's RequestLog entity.
 */
export interface RequestLog {
  id: number;
  testResultId: number;
  timestamp: string;
  url: string;
  method: string;
  statusCode: number;
  responseTimeMs: number;
  responseSize: number;
  errorMessage?: string;
  success: boolean;
  createdAt: string;
  updatedAt: string;
}

// ── Response DTOs ─────────────────────────────────────────

/**
 * Metric snapshot response — matches backend MetricSnapshotResponse.
 */
export interface MetricSnapshotResponse {
  timestamp: string;
  activeUsers: number;
  requestsPerSecond: number;
  avgResponseTimeMs: number;
  errorRate: number;
  p95Ms: number;
  cumulativeRequests: number;
  cumulativeErrors: number;
}

/**
 * Request log response — matches backend RequestLogResponse.
 */
export interface RequestLogResponse {
  id: number;
  timestamp: string;
  url: string;
  method: string;
  statusCode: number;
  responseTimeMs: number;
  responseSize: number;
  errorMessage?: string;
  success: boolean;
}

