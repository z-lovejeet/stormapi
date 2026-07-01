import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useTestResult } from '../../hooks/useTestResult';
import { useComparison } from '../../hooks/useComparison';
import * as testApi from '../../api/testApi';
import * as resultApi from '../../api/resultApi';

vi.mock('../../api/testApi');
vi.mock('../../api/resultApi');

const mockTestConfig = {
  id: 1, name: 'Test', targetUrl: 'http://localhost', httpMethod: 'GET',
  testType: 'LOAD', virtualUsers: 10, durationSeconds: 60, rampUpSeconds: 5,
  maxRetries: 3, timeoutMs: 5000, thinkTimeMs: 0, status: 'COMPLETED',
  createdAt: '2025-01-01T00:00:00Z', updatedAt: '2025-01-01T00:00:00Z',
};

const mockResult = {
  id: 1, testConfigId: 1, status: 'COMPLETED',
  totalRequests: 100, successCount: 95, failureCount: 5,
  avgResponseTimeMs: 100, minResponseTimeMs: 10, maxResponseTimeMs: 500,
  p50Ms: 80, p75Ms: 120, p90Ms: 200, p95Ms: 300, p99Ms: 450,
  throughputRps: 10, errorRate: 5, totalDataBytes: 1024,
  startedAt: '2025-01-01T00:00:00Z', completedAt: '2025-01-01T00:01:00Z',
  durationMs: 60000, createdAt: '2025-01-01T00:00:00Z',
};

describe('useTestResult', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches test config and result', async () => {
    vi.mocked(testApi.getTest).mockResolvedValue(mockTestConfig as any);
    vi.mocked(resultApi.getLatestResult).mockResolvedValue(mockResult as any);
    vi.mocked(resultApi.getSnapshots).mockResolvedValue([]);
    vi.mocked(resultApi.getStatusCodeDistribution).mockResolvedValue({ 200: 95 });
    vi.mocked(resultApi.getHistogram).mockResolvedValue([]);

    const { result } = renderHook(() => useTestResult(1));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.testConfig).toBeDefined();
    expect(result.current.result?.totalRequests).toBe(100);
    expect(result.current.statusCodes).toEqual({ 200: 95 });
    expect(result.current.error).toBeNull();
  });

  it('handles error state', async () => {
    vi.mocked(testApi.getTest).mockRejectedValue(new Error('Network error'));
    vi.mocked(resultApi.getLatestResult).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useTestResult(1));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBe('Network error');
    expect(result.current.result).toBeNull();
  });
});

describe('useComparison', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('does not fetch when IDs are null', () => {
    const { result } = renderHook(() => useComparison(null, null));

    expect(result.current.comparison).toBeNull();
    expect(result.current.loading).toBe(false);
    expect(resultApi.compareResults).not.toHaveBeenCalled();
  });

  it('fetches comparison when both IDs provided', async () => {
    const mockComparison = {
      resultA: mockResult,
      resultB: { ...mockResult, id: 2 },
      deltas: [{ field: 'throughputRps', label: 'Throughput', resultA: 10, resultB: 15, delta: 5, deltaPercent: 50, improved: true }],
    };
    vi.mocked(resultApi.compareResults).mockResolvedValue(mockComparison as any);

    const { result } = renderHook(() => useComparison(1, 2));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.comparison).toBeDefined();
    expect(result.current.comparison?.deltas).toHaveLength(1);
    expect(result.current.error).toBeNull();
  });

  it('handles comparison error', async () => {
    vi.mocked(resultApi.compareResults).mockRejectedValue(new Error('Not found'));

    const { result } = renderHook(() => useComparison(1, 999));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBe('Not found');
    expect(result.current.comparison).toBeNull();
  });
});
