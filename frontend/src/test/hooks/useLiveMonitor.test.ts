import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type { LiveMetricsMessage, TestEventMessage } from '../../types/websocket';
import type { TestConfigResponse, TestStatus, TestType, HttpMethod } from '../../types/test';

// Must mock with full relative path from the hook
vi.mock('../../api/testApi', () => ({
  getTest: vi.fn(),
}));

// Track callbacks passed to useWebSocket
let wsCallbacks: {
  onMetrics?: (m: LiveMetricsMessage) => void;
  onEvent?: (e: TestEventMessage) => void;
} = {};
const mockDisconnect = vi.fn();

vi.mock('../../hooks/useWebSocket', () => ({
  useWebSocket: vi.fn((opts: Record<string, unknown>) => {
    wsCallbacks.onMetrics = opts.onMetrics as typeof wsCallbacks.onMetrics;
    wsCallbacks.onEvent = opts.onEvent as typeof wsCallbacks.onEvent;
    return {
      connectionState: 'CONNECTED' as const,
      latestMetrics: null,
      latestLogs: [],
      latestEvent: null,
      disconnect: mockDisconnect,
    };
  }),
}));

// Import AFTER mocks
import { useLiveMonitor } from '../../hooks/useLiveMonitor';
import { getTest } from '../../api/testApi';

const mockConfig: TestConfigResponse = {
  id: 1,
  name: 'Test Load',
  targetUrl: 'http://api.example.com',
  httpMethod: 'GET' as HttpMethod,
  testType: 'LOAD' as TestType,
  virtualUsers: 50,
  durationSeconds: 120,
  rampUpSeconds: 10,
  maxRetries: 0,
  timeoutMs: 5000,
  thinkTimeMs: 0,
  status: 'RUNNING' as TestStatus,
  createdAt: '2026-06-26T10:00:00Z',
  updatedAt: '2026-06-26T10:00:00Z',
};

const mockMetrics: LiveMetricsMessage = {
  testId: 1,
  totalRequests: 500,
  successCount: 490,
  failureCount: 10,
  avgResponseTimeMs: 123.5,
  minResponseTimeMs: 10,
  maxResponseTimeMs: 500,
  p50Ms: 100,
  p75Ms: 150,
  p90Ms: 200,
  p95Ms: 300,
  p99Ms: 450,
  throughputRps: 42.3,
  errorRate: 2.0,
  activeUsers: 50,
  totalDataBytes: 1024000,
  statusCodeDistribution: { '200': 490, '500': 10 },
  timestamp: '2026-06-26T10:01:00Z',
};

describe('useLiveMonitor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    wsCallbacks = {};
    (getTest as ReturnType<typeof vi.fn>).mockResolvedValue(mockConfig);
  });

  it('fetches test config on mount', async () => {
    const { result } = renderHook(() => useLiveMonitor(1));
    expect(result.current.configLoading).toBe(true);

    // Wait for promise to resolve
    await act(async () => {});

    expect(getTest).toHaveBeenCalledWith(1);
    expect(result.current.testConfig?.name).toBe('Test Load');
    expect(result.current.configLoading).toBe(false);
  });

  it('handles config fetch error', async () => {
    (getTest as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Not found'));
    const { result } = renderHook(() => useLiveMonitor(1));

    await act(async () => {});

    expect(result.current.configError).toBe('Not found');
  });

  it('starts with zero KPIs', async () => {
    const { result } = renderHook(() => useLiveMonitor(1));
    expect(result.current.kpis.totalRequests).toBe(0);
    expect(result.current.kpis.throughputRps).toBe(0);
    // cleanup
    await act(async () => {});
  });

  it('updates KPIs when onMetrics callback fires', async () => {
    const { result } = renderHook(() => useLiveMonitor(1));
    await act(async () => {});

    act(() => {
      wsCallbacks.onMetrics?.(mockMetrics);
    });

    expect(result.current.kpis.totalRequests).toBe(500);
    expect(result.current.kpis.throughputRps).toBe(42.3);
    expect(result.current.kpis.errorRate).toBe(2.0);
  });

  it('appends chart data points on metrics', async () => {
    const { result } = renderHook(() => useLiveMonitor(1));
    await act(async () => {});

    act(() => {
      wsCallbacks.onMetrics?.(mockMetrics);
    });

    expect(result.current.chartData.responseTime).toHaveLength(1);
    expect(result.current.chartData.responseTime[0]?.value).toBe(123.5);
    expect(result.current.chartData.throughput[0]?.value).toBe(42.3);
  });

  it('enforces 60-point sliding window', async () => {
    const { result } = renderHook(() => useLiveMonitor(1));
    await act(async () => {});

    // Push 65 points
    for (let i = 0; i < 65; i++) {
      act(() => {
        wsCallbacks.onMetrics?.({ ...mockMetrics, avgResponseTimeMs: i });
      });
    }

    expect(result.current.chartData.responseTime.length).toBeLessThanOrEqual(60);
    // First point should be index 5 (shifted off 0-4)
    expect(result.current.chartData.responseTime[0]?.value).toBe(5);
  });

  it('detects completion event', async () => {
    const { result } = renderHook(() => useLiveMonitor(1));
    await act(async () => {});

    act(() => {
      wsCallbacks.onEvent?.({
        testId: 1,
        eventType: 'TEST_COMPLETED',
        message: 'Test completed',
        metadata: { resultId: 99 },
        timestamp: '2026-06-26T10:02:00Z',
      });
    });

    expect(result.current.completionState.completed).toBe(true);
    expect(result.current.completionState.status).toBe('completed');
    expect(result.current.completionState.resultId).toBe(99);
  });

  it('marks already-completed test on config load', async () => {
    (getTest as ReturnType<typeof vi.fn>).mockResolvedValue({
      ...mockConfig,
      status: 'COMPLETED',
    });

    const { result } = renderHook(() => useLiveMonitor(1));
    await act(async () => {});

    expect(result.current.completionState.completed).toBe(true);
    expect(result.current.completionState.status).toBe('completed');
  });
});
