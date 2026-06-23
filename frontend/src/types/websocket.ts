/**
 * WebSocket DTOs matching the backend broadcast payloads.
 * Used by the useWebSocket hook and dashboard components.
 */

// ── Lifecycle Events ──────────────────────────────────────────

export type TestEventType =
  | 'TEST_CREATED'
  | 'TEST_STARTED'
  | 'TEST_RUNNING'
  | 'TEST_PROGRESS'
  | 'TEST_COMPLETED'
  | 'TEST_FAILED'
  | 'TEST_CANCELLED'
  | 'TEST_STOPPED';

export interface TestEventMessage {
  testId: number;
  eventType: TestEventType;
  message: string;
  metadata: Record<string, unknown>;
  timestamp: string;
}

// ── Live Metrics ──────────────────────────────────────────────

export interface LiveMetricsMessage {
  testId: number;
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
  activeUsers: number;
  totalDataBytes: number;
  statusCodeDistribution: Record<string, number>;
  timestamp: string;
}

// ── Request Logs ──────────────────────────────────────────────

export interface RequestLogEntry {
  timestamp: string;
  url: string;
  method: string;
  statusCode: number;
  responseTimeMs: number;
  responseSize: number;
  errorMessage: string | null;
  success: boolean;
}

export interface RequestLogMessage {
  testId: number;
  entries: RequestLogEntry[];
}

// ── Connection Status ─────────────────────────────────────────

export interface ConnectionStatusMessage {
  status: 'CONNECTED' | 'DISCONNECTED';
  sessionId: string;
  activeTests: number;
  timestamp: string;
}

// ── Hook Types ────────────────────────────────────────────────

export type WebSocketConnectionState =
  | 'CONNECTING'
  | 'CONNECTED'
  | 'DISCONNECTED'
  | 'ERROR';

export interface UseWebSocketOptions {
  testId: number;
  enabled?: boolean;
  onMetrics?: (metrics: LiveMetricsMessage) => void;
  onLogBatch?: (logs: RequestLogMessage) => void;
  onEvent?: (event: TestEventMessage) => void;
  onConnectionChange?: (state: WebSocketConnectionState) => void;
}

export interface UseWebSocketReturn {
  connectionState: WebSocketConnectionState;
  latestMetrics: LiveMetricsMessage | null;
  latestLogs: RequestLogEntry[];
  latestEvent: TestEventMessage | null;
  disconnect: () => void;
}
